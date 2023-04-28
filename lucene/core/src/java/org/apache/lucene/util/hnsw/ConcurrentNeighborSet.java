package org.apache.lucene.util.hnsw;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.apache.lucene.util.NumericUtils;

/**
 * A concurrent set of neighbors
 *
 * <p>Neighbors are stored in a concurrent navigable set by encoding ordinal and score together in a
 * long. This means we can quickly iterate either forwards, or backwards.
 *
 * <p>The maximum connection count is loosely maintained -- meaning, we tolerate temporarily
 * exceeding the max size by a number of elements up to the number of threads performing concurrent
 * inserts, but it will always be reduced back to the cap immediately afterwards. This avoids taking
 * out a Big Lock to impose a strict cap.
 */
public class ConcurrentNeighborSet {
  private final ConcurrentSkipListSet<Long> neighbors;
  private final int maxConnections;
  private final AtomicInteger size;

  public ConcurrentNeighborSet(int maxConnections) {
    this.maxConnections = maxConnections;
    neighbors = new ConcurrentSkipListSet<>(Comparator.<Long>naturalOrder().reversed());
    size = new AtomicInteger();
  }

  public Iterator<Integer> nodeIterator() {
    return neighbors.stream().map(ConcurrentNeighborSet::decodeNodeId).iterator();
  }

  public int size() {
    return size.get();
  }

  public Stream<Entry<Float, Integer>> stream() {
    return neighbors.stream()
        .map(encoded -> Map.entry(decodeScore(encoded), decodeNodeId(encoded)));
  }

  /**
   * For each candidate (going from best to worst), select it only if it is closer to target than it
   * is to any of the already-selected neighbors. This is maintained whether those other neighbors
   * were selected by this method, or were added as a "backlink" to a node inserted concurrently
   * that chose this one as a neighbor.
   */
  public void insertDiverse(
      NeighborArray candidates, BiFunction<Integer, Integer, Float> scoreBetween) {
    for (int i = candidates.size() - 1; neighbors.size() < maxConnections && i >= 0; i--) {
      int cNode = candidates.node[i];
      float cScore = candidates.score[i];
      // TODO in the paper, the diversity requirement is only enforced when there are more than
      // maxConn
      if (isDiverse(cNode, cScore, scoreBetween)) {
        // raw inserts (invoked by other threads inserting neighbors) could happen concurrently,
        // so don't "cheat" and do a raw put()
        insert(cNode, cScore, scoreBetween);
      }
    }
    // TODO follow the paper's suggestion and fill up the rest of the neighbors with non-diverse
    // candidates?
  }

  /**
   * Insert a new neighbor, maintaining our size cap by removing the least diverse neighbor if
   * necessary.
   */
  public void insert(int node, float score, BiFunction<Integer, Integer, Float> scoreBetween) {
    neighbors.add(encode(node, score));
    if (size.incrementAndGet() > maxConnections) {
      removeLeastDiverse(scoreBetween);
      size.decrementAndGet();
    }
  }

  // is the candidate node with the given score closer to the base node than it is to any of the
  // existing neighbors
  private boolean isDiverse(
      int node, float score, BiFunction<Integer, Integer, Float> scoreBetween) {
    return stream().noneMatch(e -> scoreBetween.apply(e.getValue(), node) > score);
  }

  /**
   * find the first node e1 starting with the last neighbor (i.e. least similar to the base node),
   * look at all nodes e2 that are closer to the base node than e1 is. if any e2 is closer to e1
   * than e1 is to the base node, remove e1.
   */
  private void removeLeastDiverse(BiFunction<Integer, Integer, Float> scoreBetween) {
    for (var e1 : neighbors.descendingSet()) {
      var e1Id = decodeNodeId(e1);
      var baseScore = decodeScore(e1);

      var e2Iterator = iteratorStartingAfter(neighbors, e1);
      while (e2Iterator.hasNext()) {
        var e2 = e2Iterator.next();
        var e2Id = decodeNodeId(e2);
        var e1e2Score = scoreBetween.apply(e1Id, e2Id);
        if (e1e2Score >= baseScore) {
          if (neighbors.remove(e1)) {
            return;
          }
          // else another thread already removed it, keep looking
        }
      }
    }
    // couldn't find any "non-diverse" neighbors, so remove the one farthest from the base node
    neighbors.remove(neighbors.last());
  }

  /**
   * Returns an iterator over the entries in the set, starting at the entry *after* the given key.
   * So iteratorStartingAfter(map, 2) invoked on a set with keys [1, 2, 3, 4] would return an
   * iterator over the entries [3, 4].
   */
  private static <K> Iterator<K> iteratorStartingAfter(NavigableSet<K> set, K key) {
    // this isn't ideal, since the iteration will be worst case O(N log N), but since the worst
    // scores will usually be the first ones we iterate through, the average case is much better
    return new Iterator<>() {
      private K nextItem = set.lower(key);

      @Override
      public boolean hasNext() {
        return nextItem != null;
      }

      @Override
      public K next() {
        K current = nextItem;
        nextItem = set.lower(nextItem);
        return current;
      }
    };
  }

  public boolean contains(int i) {
    for (var e : neighbors) {
      if (decodeNodeId(e) == i) {
        return true;
      }
    }
    return false;
  }

  // as found in NeighborQueue
  static long encode(int node, float score) {
    return (((long) NumericUtils.floatToSortableInt(score)) << 32) | (0xFFFFFFFFL & ~node);
  }

  static float decodeScore(long heapValue) {
    return NumericUtils.sortableIntToFloat((int) (heapValue >> 32));
  }

  static int decodeNodeId(long heapValue) {
    return (int) ~(heapValue);
  }
}