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
package org.anchoranalysis.plugin.ml.bean.classifier;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import org.anchoranalysis.feature.bean.list.FeatureList;
import org.anchoranalysis.feature.bean.list.FeatureListFactory;
import org.anchoranalysis.feature.bean.operator.FeatureListElem;
import org.anchoranalysis.feature.calculate.FeatureCalculationException;
import org.anchoranalysis.feature.calculate.NamedFeatureCalculateException;
import org.anchoranalysis.feature.calculate.cache.SessionInput;
import org.anchoranalysis.feature.input.FeatureInput;
import org.anchoranalysis.feature.results.ResultsVector;

/**
 * This is actually violating the Bean Rules for the feature
 *
 * @author Owen Feehan
 */
class FeatureSVMClassifier<T extends FeatureInput> extends FeatureListElem<T> {

    // What we take in
    private final svm_model model;

    /**
     * Indicates the direction of the decision-making value. If true, the decision-value should be
     * >0 for Label 1. If false, it's the opposite.
     */
    private final boolean direction;

    public FeatureSVMClassifier(svm_model model, FeatureList<T> featureList, boolean direction) {
        super();
        this.model = model;
        this.direction = direction;
        setList(featureList);
    }

    @Override
    public FeatureSVMClassifier<T> duplicateBean() {
        return new FeatureSVMClassifier<>(
                model, FeatureListFactory.wrapDuplicate(getList()), direction);
    }

    @Override
    public double calculate(SessionInput<T> input) throws FeatureCalculationException {

        ResultsVector results;
        try {
            results = input.calculate(FeatureListFactory.wrapReuse(getList()));
        } catch (NamedFeatureCalculateException e) {
            throw new FeatureCalculationException(e);
        }

        svm_node[] nodes = convert(results);

        double[] arrPredictValues = new double[1];
        svm.svm_predict_values(model, nodes, arrPredictValues);
        double predictValue = arrPredictValues[0];

        if (direction) {
            return predictValue;
        } else {
            return predictValue * -1;
        }
    }

    private static svm_node[] convert(ResultsVector results) {

        svm_node[] out = new svm_node[results.length()];

        for (int i = 0; i < results.length(); i++) {
            out[i] = new svm_node();
            out[i].index = i + 1;
            out[i].value = results.get(i);
        }

        return out;
    }
}
