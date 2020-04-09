package org.anchoranalysis.plugin.ml.bean;

/*
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2019 F. Hoffmann-La Roche Ltd
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


import java.util.ArrayList;
import java.util.List;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.shared.params.keyvalue.KeyValueParamsProvider;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.core.params.KeyValueParams;
import org.anchoranalysis.feature.bean.Feature;
import org.anchoranalysis.feature.bean.list.FeatureList;
import org.anchoranalysis.feature.bean.list.FeatureListProviderReferencedFeatures;
import org.anchoranalysis.feature.bean.operator.Constant;
import org.anchoranalysis.feature.bean.operator.Reference;
import org.anchoranalysis.feature.bean.operator.Sum;
import org.anchoranalysis.feature.calc.params.FeatureCalcParams;

import ch.ethz.biol.cell.mpp.nrg.feature.operator.IfGreaterThan;
import ch.ethz.biol.cell.mpp.nrg.feature.operator.MultiplyByConstant;

public class FeatureListProviderLDAClassifier extends FeatureListProviderReferencedFeatures<FeatureCalcParams> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// START BEAN PROPERTIES
	@BeanField 
	private KeyValueParamsProvider keyValueParamsProvider;
	// END BEAN PROPERTIES
	
	private static String ldaThresholdKey = "__ldaThreshold";
	
	private static String featureNameScore = "ldaScore";
	private static String featureNameThreshold = "ldaThreshold";
	private static String featureNameClassifier = "ldaClassifier";
	
	private void checkForMissingFeatures( KeyValueParams kpv ) throws CreateException {
		
		List<String> list = new ArrayList<String>();
		
		// For now let's just check all the feature are present
		for( String name : kpv.keySet() ) {
			
			// Skip the threshold name
			if (name.equals(ldaThresholdKey)) {
				continue;
			}
			
			Feature<FeatureCalcParams> feature = getSharedObjects().getSharedFeatureSet().getNull(name);
			if (feature==null) {
				list.add(name);
			}
		}
		
		if (list.size()>0) {
			throw createExceptionForMissingStrings( list );
		}
	}
	
	static CreateException createExceptionForMissingStrings( List<String> listNames ) {
		// Then we have at least one missing feature, throw an exception
		StringBuilder sb = new StringBuilder();
		
		sb.append( "The following features referenced in the model are missing:" );
		sb.append( System.lineSeparator() );
		
		for( String featureName : listNames ) {
			sb.append( featureName );
			sb.append( System.lineSeparator() );
		}
		
//		sb.append( System.lineSeparator() );
//		sb.append( System.lineSeparator() );
//		
//		sb.append("The following features are available:");
//		sb.append( System.lineSeparator() );
//		
//		for( String featureName : getSharedObjects().getSharedFeatureSet().keys() ) {
//			sb.append( featureName );
//			sb.append( System.lineSeparator() );
//		}
		return new CreateException( sb.toString() );		
	}
	
	private Feature<FeatureCalcParams> createScoreFeature( KeyValueParams kpv ) throws CreateException {
		
		Sum<FeatureCalcParams> sum = new Sum<>();
		sum.setIgnoreNaN(true);
		
		// For now let's just check all the feature are present
		for( String name : kpv.keySet() ) {
			
			// Skip the threshold name
			if (name.equals(ldaThresholdKey)) {
				continue;
			}
			
			Feature<FeatureCalcParams> feature = getSharedObjects().getSharedFeatureSet().getNull(name);
			sum.getList().add(
				new MultiplyByConstant<FeatureCalcParams>(feature, kpv.getPropertyAsDouble(name))
			);
		}
		
		sum.setCustomName(featureNameScore);
		return sum;
		

	}
	
	private Feature<FeatureCalcParams> createThresholdFeature( double threshold ) {
		Constant<FeatureCalcParams> out = new Constant<>(threshold);
		out.setCustomName(featureNameThreshold);
		return out;
	}
	
	private Feature<FeatureCalcParams> createClassifierFeature( double threshold ) {
		// If we are below the the threshold we output -1
		
		Reference score = new Reference(featureNameScore);
		
		IfGreaterThan<FeatureCalcParams> featThresh = new IfGreaterThan<>();
		featThresh.setFeatureCondition(score );
		featThresh.setItem( new Constant<>(1) );
		featThresh.setFeatureElse( new Constant<>(0) );
		featThresh.setValue( threshold );
				
		featThresh.setCustomName(featureNameClassifier);
		return featThresh;		
	}
	
	@Override
	public FeatureList<FeatureCalcParams> create() throws CreateException {

		KeyValueParams kpv = keyValueParamsProvider.create();
		
		if (kpv==null) {
			throw new CreateException("Cannot find KeyValueParams for LDA Model");
		}
		
		checkForMissingFeatures( kpv );
		
		Feature<FeatureCalcParams> featScore = createScoreFeature(kpv);
		
		double threshold = kpv.getPropertyAsDouble(ldaThresholdKey);
		
		Feature<FeatureCalcParams> featThreshold = createThresholdFeature(threshold);
		
		Feature<FeatureCalcParams> featClassifier = createClassifierFeature(threshold);
		
		FeatureList<FeatureCalcParams> out = new FeatureList<>();
		out.add(featScore);
		out.add(featThreshold);
		out.add(featClassifier);
		return out;
	}
	


	public KeyValueParamsProvider getKeyValueParamsProvider() {
		return keyValueParamsProvider;
	}

	public void setKeyValueParamsProvider(
			KeyValueParamsProvider keyValueParamsProvider) {
		this.keyValueParamsProvider = keyValueParamsProvider;
	}
}
