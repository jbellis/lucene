/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util.hnsw.math.distribution;

import org.apache.lucene.util.hnsw.math.exception.NotStrictlyPositiveException;
import org.apache.lucene.util.hnsw.math.exception.util.LocalizedFormats;
import org.apache.lucene.util.hnsw.math.random.RandomGenerator;


public abstract class AbstractMultivariateRealDistribution
    implements MultivariateRealDistribution {
    
    protected final RandomGenerator random;
    
    private final int dimension;

    
    protected AbstractMultivariateRealDistribution(RandomGenerator rng,
                                                   int n) {
        random = rng;
        dimension = n;
    }

    
    public void reseedRandomGenerator(long seed) {
        random.setSeed(seed);
    }

    
    public int getDimension() {
        return dimension;
    }

    
    public abstract double[] sample();

    
    public double[][] sample(final int sampleSize) {
        if (sampleSize <= 0) {
            throw new NotStrictlyPositiveException(LocalizedFormats.NUMBER_OF_SAMPLES,
                                                   sampleSize);
        }
        final double[][] out = new double[sampleSize][dimension];
        for (int i = 0; i < sampleSize; i++) {
            out[i] = sample();
        }
        return out;
    }
}