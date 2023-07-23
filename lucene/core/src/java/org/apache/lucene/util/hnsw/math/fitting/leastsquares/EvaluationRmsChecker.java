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
package org.apache.lucene.util.hnsw.math.fitting.leastsquares;

import org.apache.lucene.util.hnsw.math.fitting.leastsquares.LeastSquaresProblem.Evaluation;
import org.apache.lucene.util.hnsw.math.optim.ConvergenceChecker;
import org.apache.lucene.util.hnsw.math.util.Precision;


public class EvaluationRmsChecker implements ConvergenceChecker<Evaluation> {

    
    private final double relTol;
    
    private final double absTol;

    
    public EvaluationRmsChecker(final double tol) {
        this(tol, tol);
    }

    
    public EvaluationRmsChecker(final double relTol, final double absTol) {
        this.relTol = relTol;
        this.absTol = absTol;
    }

    
    public boolean converged(final int iteration,
                             final Evaluation previous,
                             final Evaluation current) {
        final double prevRms = previous.getRMS();
        final double currRms = current.getRMS();
        return Precision.equals(prevRms, currRms, this.absTol) ||
                Precision.equalsWithRelativeTolerance(prevRms, currRms, this.relTol);
    }

}
