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

package org.apache.lucene.util.hnsw.math.ode.sampling;


public enum StepNormalizerBounds {
    
    NEITHER(false, false),

    
    FIRST(true, false),

    
    LAST(false, true),

    
    BOTH(true, true);

    
    private final boolean first;

    
    private final boolean last;

    
    StepNormalizerBounds(final boolean first, final boolean last) {
        this.first = first;
        this.last = last;
    }

    
    public boolean firstIncluded() {
        return first;
    }

    
    public boolean lastIncluded() {
        return last;
    }
}