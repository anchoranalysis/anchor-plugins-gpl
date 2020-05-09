package org.anchoranalysis.plugin.ml.bean;

/*-
 * #%L
 * anchor-plugin-ml
 * %%
 * Copyright (C) 2016 - 2020 ETH Zurich, University of Zurich, Owen Feehan
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.anchoranalysis.feature.bean.list.FeatureList;
import org.anchoranalysis.feature.bean.operator.FeatureListElem;
import org.anchoranalysis.feature.cache.SessionInput;
import org.anchoranalysis.feature.calc.FeatureCalcException;
import org.anchoranalysis.feature.calc.results.ResultsVector;
import org.anchoranalysis.feature.input.FeatureInput;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

/**
 * This is actually violating the Bean Rules for the feature
 * 
 * @author Owen Feehan
 *
 */
class FeatureSVMClassifier<T extends FeatureInput> extends FeatureListElem<T> {

	// What we take in
	private svm_model model;
	
	/**
	 * Indicates the direction of the decision-making value. If TRUE, the decision-value should be >0 for Label 1.  If false, it's the opposite.
	 */
	private boolean direction;
	
	public FeatureSVMClassifier(svm_model model,
			FeatureList<T> featureList, boolean direction ) {
		super();
		this.model = model;
		setList(featureList);
		this.direction = direction;
	}
	

	@Override
	public FeatureSVMClassifier<T> duplicateBean() {
		return new FeatureSVMClassifier<>(
			model,
			new FeatureList<>(getList()).duplicateBean(),
			direction
		);
	}

	@Override
	public double calc(SessionInput<T> input)
			throws FeatureCalcException {
		
		ResultsVector rv = input.calc( getList() );
		
		svm_node[] nodes = convert(rv);
		
		double[] arrPredictValues = new double[1];
		svm.svm_predict_values(model, nodes, arrPredictValues );
		double predictValue = arrPredictValues[0];
		
		assert( !Double.isNaN(predictValue) );
		
		if (direction) {
			return predictValue;
		} else {
			return predictValue * -1;
		}
	}
	
	private svm_node[] convert( ResultsVector rv ) {
		
		svm_node[] out = new svm_node[ rv.length() ];
		
		for(int i=0;i<rv.length(); i++)
		{
			out[i] = new svm_node();
			out[i].index = i+1;
			out[i].value = rv.get(i);
		}
		
		return out;
	}
}
