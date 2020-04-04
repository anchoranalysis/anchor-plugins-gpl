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


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.core.index.GetOperationFailedException;
import org.anchoranalysis.core.name.provider.INamedProvider;
import org.anchoranalysis.core.name.provider.NamedProviderGetException;
import org.anchoranalysis.feature.bean.Feature;
import org.anchoranalysis.feature.bean.list.FeatureList;
import org.anchoranalysis.feature.bean.list.FeatureListProviderReferencedFeatures;
import org.anchoranalysis.feature.bean.operator.Constant;
import org.anchoranalysis.feature.bean.operator.FeatureListElem;
import org.anchoranalysis.feature.cache.CacheableParams;
import org.anchoranalysis.feature.calc.FeatureCalcException;
import org.anchoranalysis.feature.calc.ResultsVector;
import org.anchoranalysis.feature.calc.params.FeatureCalcParams;
import org.anchoranalysis.feature.name.FeatureNameList;
import org.anchoranalysis.io.bean.filepath.provider.FilePathProvider;
import org.anchoranalysis.io.csv.reader.CSVReaderByLine;
import org.anchoranalysis.io.csv.reader.CSVReaderByLine.ReadByLine;
import org.anchoranalysis.math.statistics.FirstSecondOrderStatistic;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import ch.ethz.biol.cell.mpp.nrg.feature.operator.ZScore;

public class FeatureListProviderSVMClassifier extends FeatureListProviderReferencedFeatures {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// START BEAN PROPERTIES
	@BeanField
	private FilePathProvider filePathProviderSVM;
	
	/**
	 * Normalize features
	 */
	@BeanField
	private boolean normalizeFeatures = true;
	
	// Multiples the decision value by -1.  Useful for when the feature is used in a minimization/maximization routine.
	@BeanField
	private boolean invertDecisionValue = false;
	// END BEAN PROPERTIES

	
	
	private FeatureNameList readFeatureNames( Path filePath ) throws FileNotFoundException, IOException {
		
		FeatureNameList out = new FeatureNameList();
		
		try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       out.add( line );
		    }
		}
		
		return out;
	}
	
	private static List<FirstSecondOrderStatistic> readScale( Path filePath ) throws IOException {
		
		List<FirstSecondOrderStatistic> out = new ArrayList<>();
		
		try( ReadByLine reader = CSVReaderByLine.open(filePath, " ", false)) {
			reader.read(
				(line,firstLine) -> {
					FirstSecondOrderStatistic stat = new FirstSecondOrderStatistic();
					stat.setMean( Double.parseDouble(line[0] ) );
					stat.setScale( Double.parseDouble(line[1] ) );
					out.add(stat);	
				}
			);
		}
				
		return out;
	}
	
	private static Path differentEnding( Path fileSVM, String ending ) {
		String strReplaced = fileSVM.toString().replaceAll("\\.svm$", ending );
		return Paths.get( strReplaced );
	}
	
	@Override
	public FeatureList create() throws CreateException {
		
		assert( getSharedObjects()!=null );
		assert( getSharedObjects().getSharedFeatureSet()!=null );
		
		try {
			Path fileSVM = filePathProviderSVM.create();
				
			FeatureList features = findModelFeatures( fileSVM );

			Feature featureClassify = buildClassifierFeature(fileSVM, features);
			
			return wrapInList(featureClassify);
			
		} catch (OperationFailedException e) {
			throw new CreateException(e);
		}
	}
	
	private Feature buildClassifierFeature( Path fileSVM, FeatureList features ) throws OperationFailedException {
		try {
			svm_model model = svm.svm_load_model(fileSVM.toString());
			
			// Assume two-labelling problem, and get the direction we want from the labels
			int[] labels = model.label;
			boolean ascendingLabels = (labels[0]<labels[1]);
			
			boolean direction = invertDecisionValue ? ascendingLabels : !ascendingLabels;
			
			FeatureSVMClassifier featureClassify = new FeatureSVMClassifier( model, features, direction );
			featureClassify.setCustomName("svmClassifier");
			return featureClassify;
		} catch (IOException e) {
			throw new OperationFailedException(e);
		}
	}
	
	private FeatureList findModelFeatures( Path fileSVM ) throws OperationFailedException {
		try {
			Path filePathFeatures = differentEnding( fileSVM, ".features");
			FeatureNameList featureNames = readFeatureNames(filePathFeatures);
			
			Path filePathScale = differentEnding( fileSVM, ".scale");
			List<FirstSecondOrderStatistic> listStats = readScale(filePathScale);
	
			return listFromNames(featureNames, getSharedObjects().getSharedFeatureSet(), listStats );
		} catch (IOException e) {
			throw new OperationFailedException(e);
		}
	}
	
	private static FeatureList wrapInList( Feature f ) {
		FeatureList out = new FeatureList();
		out.add(f);
		return out;
	}
	
	private FeatureList listFromNames( FeatureNameList featureNames, INamedProvider<Feature> allFeatures, List<FirstSecondOrderStatistic> listStats ) throws OperationFailedException {
		
		FeatureList out = new FeatureList();
		
		List<String> missing = new ArrayList<String>();
		
		assert( listStats.size()==featureNames.size() );
		
		for( int i=0; i<featureNames.size(); i++ ) {
			
			try {
				addOutOrMissing(
					featureNames.get(i),
					listStats.get(i),
					allFeatures,				
					out,
					missing
				);
			} catch (NamedProviderGetException e) {
				throw new OperationFailedException(e.summarize());
			}
		}
		
		if (missing.size()>0) {
			// Embed exception
			try {
				throw FeatureListProviderLDAClassifier.createExceptionForMissingStrings(missing);
			} catch (CreateException e) {
				throw new OperationFailedException(e);
			}
		}
		
		return out;
	}
	
	/** Adds a feature to an out-list if it exists, or adds its name to a missing-list otherwise 
	 * @throws GetOperationFailedException */
	private void addOutOrMissing( String featureName, FirstSecondOrderStatistic stat, INamedProvider<Feature> allFeatures, FeatureList out, List<String> missing ) throws NamedProviderGetException {
		Feature feature = allFeatures.getNull(featureName);
		if (feature!=null) {
			out.add(
				maybeNormalise(feature, stat )
			);
		} else {
			missing.add(featureName);
		}
	}

	
	private Feature maybeNormalise( Feature feature, FirstSecondOrderStatistic stat ) {
		if (normalizeFeatures) {
			return createScaledFeature(feature,stat);
		} else {
			return feature;
		}
	}
	
	
	private static Feature createScaledFeature( Feature feature, FirstSecondOrderStatistic stat ) {
		
		Constant mean = new Constant(stat.getMean());
		mean.setCustomName("mean");
		
		Constant stdDev = new Constant(stat.getScale() );
		stdDev.setCustomName("stdDev");
		
		ZScore featureNormalized = new ZScore();
		featureNormalized.setItem( feature );
		featureNormalized.setItemMean( mean );
		featureNormalized.setItemStdDev( stdDev );
		featureNormalized.setCustomName( feature.getCustomName() + " (scaled)");
		return featureNormalized;
	}
	
	
	/**
	 * This is actually violating the Bean Rules for the feature
	 * 
	 * @author Owen Feehan
	 *
	 */
	private static class FeatureSVMClassifier extends FeatureListElem {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		// What we take in
		private svm_model model;
		
		/**
		 * Indicates the direction of the decision-making value. If TRUE, the decision-value should be >0 for Label 1.  If false, it's the opposite.
		 */
		private boolean direction;
		
		public FeatureSVMClassifier(svm_model model,
				FeatureList featureList, boolean direction ) {
			super();
			this.model = model;
			setList(featureList);
			this.direction = direction;
		}
		

		@Override
		public Feature duplicateBean() {
			return new FeatureSVMClassifier( model, new FeatureList(getList()).duplicateBean(), direction );
		}
		
		// DEBUG METHOD
//		@SuppressWarnings("unused")
//		private void writeCSV( String name, ResultsVector rv ) {
//			TempBoundOutputManager output = TempBoundOutputManager.instance();
//			FeatureListCSVGeneratorHorizontal generator =new FeatureListCSVGeneratorHorizontal("ff", new FeatureList(getList()).createNames() );
//			generator.setIterableElement(  new ResultsVectorCollection(rv) );
//			output.getBoundOutputManager().getWriterAlwaysAllowed().write(name, generator );
//		}
	
		@Override
		public double calc(CacheableParams<? extends FeatureCalcParams> params)
				throws FeatureCalcException {
			
			ResultsVector rv = params.calc( getList() );

// DEBUG CODE 
//			writeCSV("scaled",rv);
//			
//			FeatureList subFeatures = new FeatureList(getList().stream().map( a-> ((ZScore) a).getItem() ).collect( Collectors.toList() ));
//			ResultsVector rvUnscaled = cache.calcSuppressErrors( subFeatures, params);
//			
//			writeCSV("unscaled",rvUnscaled);
//			
//			
//			
//			Feature featureEdge = subFeatures.find("pair.objectEdgeIntersectionIntensityMaxSlice.1").duplicateBean();
//			SequentialSession sessionDebug = new SequentialSession( featureEdge);
//			try {
//				sessionDebug.start( new FeatureInitParams(), new SharedFeatureList(), FeatureSVMClassifier.this.getLogger() );
//			} catch (InitException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			ResultsVector rvDebug = sessionDebug.calc(params);
//			
//			FeatureObjMaskPairMergedParams paramsCast = (FeatureObjMaskPairMergedParams) params;
//			FeatureObjMaskPairMergedParams paramsRev = new FeatureObjMaskPairMergedParams( paramsCast.getObjMask2(), paramsCast.getObjMask1(), paramsCast.getObjMaskMerged() );
//			paramsRev.setNrgStack( paramsCast.getNrgStack() );
//			
//			ResultsVector rvDebug2 = sessionDebug.calc(paramsRev);
			
			
			
			svm_node[] nodes = convert(rv);
			
			double[] arrPredictValues = new double[1];
			svm.svm_predict_values(model, nodes, arrPredictValues );
			double predictValue = arrPredictValues[0];
			
			//System.out.printf("Rv[50]=%f Rv[51]=%f Rv[52]=%f   predict=%f\n", rv.get(50), rv.get(51), rv.get(52), predictValue );
			//System.out.printf(" predict=%f\n",  predictValue );
			
//			// Assume two class problem
//			if (predict_values[0] >= 0) {
//				return 2;
//			} else {
//				return 1;
//			}
			
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

	public FilePathProvider getFileProviderSVM() {
		return filePathProviderSVM;
	}

	public void setFileProviderSVM(FilePathProvider fileProviderSVM) {
		this.filePathProviderSVM = fileProviderSVM;
	}

	public boolean isNormalizeFeatures() {
		return normalizeFeatures;
	}

	public void setNormalizeFeatures(boolean normalizeFeatures) {
		this.normalizeFeatures = normalizeFeatures;
	}

	public boolean isInvertDecisionValue() {
		return invertDecisionValue;
	}

	public void setInvertDecisionValue(boolean invertDecisionValue) {
		this.invertDecisionValue = invertDecisionValue;
	}

	public FilePathProvider getFilePathProviderSVM() {
		return filePathProviderSVM;
	}

	public void setFilePathProviderSVM(FilePathProvider filePathProviderSVM) {
		this.filePathProviderSVM = filePathProviderSVM;
	}
}
