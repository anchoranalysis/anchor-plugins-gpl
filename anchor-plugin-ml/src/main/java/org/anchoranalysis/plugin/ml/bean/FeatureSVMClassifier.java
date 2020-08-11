/*-
 * #%L
 * anchor-plugin-ml
 * %%
 * Copyright (C) 2010 - 2020 Owen Feehan, ETH Zurich, University of Zurich, Hoffmann-La Roche
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package org.anchoranalysis.plugin.ml.bean;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import org.anchoranalysis.feature.bean.list.FeatureList;
import org.anchoranalysis.feature.bean.list.FeatureListFactory;
import org.anchoranalysis.feature.bean.operator.FeatureListElem;
import org.anchoranalysis.feature.cache.SessionInput;
import org.anchoranalysis.feature.calc.FeatureCalculationException;
import org.anchoranalysis.feature.calc.NamedFeatureCalculationException;
import org.anchoranalysis.feature.calc.results.ResultsVector;
import org.anchoranalysis.feature.input.FeatureInput;

/**
 * This is actually violating the Bean Rules for the feature
 *
 * @author Owen Feehan
 */
class FeatureSVMClassifier<T extends FeatureInput> extends FeatureListElem<T> {

    // What we take in
    private svm_model model;

    /**
     * Indicates the direction of the decision-making value. If TRUE, the decision-value should be
     * >0 for Label 1. If false, it's the opposite.
     */
    private boolean direction;

    public FeatureSVMClassifier(svm_model model, FeatureList<T> featureList, boolean direction) {
        super();
        this.model = model;
        setList(featureList);
        this.direction = direction;
    }

    @Override
    public FeatureSVMClassifier<T> duplicateBean() {
        return new FeatureSVMClassifier<>(
                model, FeatureListFactory.wrapDuplicate(getList()), direction);
    }

    @Override
    public double calc(SessionInput<T> input) throws FeatureCalculationException {

        ResultsVector rv;
        try {
            rv = input.calc(FeatureListFactory.wrapReuse(getList()));
        } catch (NamedFeatureCalculationException e) {
            throw new FeatureCalculationException(e);
        }

        svm_node[] nodes = convert(rv);

        double[] arrPredictValues = new double[1];
        svm.svm_predict_values(model, nodes, arrPredictValues);
        double predictValue = arrPredictValues[0];

        if (direction) {
            return predictValue;
        } else {
            return predictValue * -1;
        }
    }

    private svm_node[] convert(ResultsVector rv) {

        svm_node[] out = new svm_node[rv.length()];

        for (int i = 0; i < rv.length(); i++) {
            out[i] = new svm_node();
            out[i].index = i + 1;
            out[i].value = rv.get(i);
        }

        return out;
    }
}
