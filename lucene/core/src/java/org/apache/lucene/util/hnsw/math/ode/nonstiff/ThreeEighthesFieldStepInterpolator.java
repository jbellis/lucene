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

package org.apache.lucene.util.hnsw.math.ode.nonstiff;

import org.apache.lucene.util.hnsw.math.Field;
import org.apache.lucene.util.hnsw.math.RealFieldElement;
import org.apache.lucene.util.hnsw.math.ode.FieldEquationsMapper;
import org.apache.lucene.util.hnsw.math.ode.FieldODEStateAndDerivative;



class ThreeEighthesFieldStepInterpolator<T extends RealFieldElement<T>>
      extends RungeKuttaFieldStepInterpolator<T> {

    
    ThreeEighthesFieldStepInterpolator(final Field<T> field, final boolean forward,
                                       final T[][] yDotK,
                                       final FieldODEStateAndDerivative<T> globalPreviousState,
                                       final FieldODEStateAndDerivative<T> globalCurrentState,
                                       final FieldODEStateAndDerivative<T> softPreviousState,
                                       final FieldODEStateAndDerivative<T> softCurrentState,
                                       final FieldEquationsMapper<T> mapper) {
        super(field, forward, yDotK,
              globalPreviousState, globalCurrentState, softPreviousState, softCurrentState,
              mapper);
    }

    
    @Override
    protected ThreeEighthesFieldStepInterpolator<T> create(final Field<T> newField, final boolean newForward, final T[][] newYDotK,
                                                           final FieldODEStateAndDerivative<T> newGlobalPreviousState,
                                                           final FieldODEStateAndDerivative<T> newGlobalCurrentState,
                                                           final FieldODEStateAndDerivative<T> newSoftPreviousState,
                                                           final FieldODEStateAndDerivative<T> newSoftCurrentState,
                                                           final FieldEquationsMapper<T> newMapper) {
        return new ThreeEighthesFieldStepInterpolator<T>(newField, newForward, newYDotK,
                                                         newGlobalPreviousState, newGlobalCurrentState,
                                                         newSoftPreviousState, newSoftCurrentState,
                                                         newMapper);
    }

    
    @SuppressWarnings("unchecked")
    @Override
    protected FieldODEStateAndDerivative<T> computeInterpolatedStateAndDerivatives(final FieldEquationsMapper<T> mapper,
                                                                                   final T time, final T theta,
                                                                                   final T thetaH, final T oneMinusThetaH) {

        final T coeffDot3  = theta.multiply(0.75);
        final T coeffDot1  = coeffDot3.multiply(theta.multiply(4).subtract(5)).add(1);
        final T coeffDot2  = coeffDot3.multiply(theta.multiply(-6).add(5));
        final T coeffDot4  = coeffDot3.multiply(theta.multiply(2).subtract(1));
        final T[] interpolatedState;
        final T[] interpolatedDerivatives;

        if (getGlobalPreviousState() != null && theta.getReal() <= 0.5) {
            final T s          = thetaH.divide(8);
            final T fourTheta2 = theta.multiply(theta).multiply(4);
            final T coeff1     = s.multiply(fourTheta2.multiply(2).subtract(theta.multiply(15)).add(8));
            final T coeff2     = s.multiply(theta.multiply(5).subtract(fourTheta2)).multiply(3);
            final T coeff3     = s.multiply(theta).multiply(3);
            final T coeff4     = s.multiply(fourTheta2.subtract(theta.multiply(3)));
            interpolatedState       = previousStateLinearCombination(coeff1, coeff2, coeff3, coeff4);
            interpolatedDerivatives = derivativeLinearCombination(coeffDot1, coeffDot2, coeffDot3, coeffDot4);
        } else {
            final T s          = oneMinusThetaH.divide(-8);
            final T fourTheta2 = theta.multiply(theta).multiply(4);
            final T thetaPlus1 = theta.add(1);
            final T coeff1     = s.multiply(fourTheta2.multiply(2).subtract(theta.multiply(7)).add(1));
            final T coeff2     = s.multiply(thetaPlus1.subtract(fourTheta2)).multiply(3);
            final T coeff3     = s.multiply(thetaPlus1).multiply(3);
            final T coeff4     = s.multiply(thetaPlus1.add(fourTheta2));
            interpolatedState       = currentStateLinearCombination(coeff1, coeff2, coeff3, coeff4);
            interpolatedDerivatives = derivativeLinearCombination(coeffDot1, coeffDot2, coeffDot3, coeffDot4);
        }

        return new FieldODEStateAndDerivative<T>(time, interpolatedState, interpolatedDerivatives);

    }

}