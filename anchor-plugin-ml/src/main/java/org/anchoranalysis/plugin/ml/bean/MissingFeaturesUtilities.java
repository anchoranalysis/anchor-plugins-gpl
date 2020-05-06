package org.anchoranalysis.plugin.ml.bean;

import java.util.List;

import org.anchoranalysis.core.error.CreateException;

class MissingFeaturesUtilities {

	private MissingFeaturesUtilities() {}
		
	public static CreateException createExceptionForMissingStrings( List<String> listNames ) {
		// Then we have at least one missing feature, throw an exception
		StringBuilder sb = new StringBuilder();
		
		sb.append( "The following features referenced in the model are missing:" );
		sb.append( System.lineSeparator() );
		
		for( String featureName : listNames ) {
			sb.append( featureName );
			sb.append( System.lineSeparator() );
		}

		return new CreateException( sb.toString() );		
	}
}