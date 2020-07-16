/* (C)2020 */
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
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import libsvm.svm;
import libsvm.svm_model;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.core.index.GetOperationFailedException;
import org.anchoranalysis.core.name.provider.NamedProvider;
import org.anchoranalysis.core.name.provider.NamedProviderGetException;
import org.anchoranalysis.feature.bean.Feature;
import org.anchoranalysis.feature.bean.list.FeatureList;
import org.anchoranalysis.feature.bean.list.FeatureListFactory;
import org.anchoranalysis.feature.bean.list.FeatureListProviderReferencedFeatures;
import org.anchoranalysis.feature.bean.operator.Constant;
import org.anchoranalysis.feature.input.FeatureInput;
import org.anchoranalysis.feature.name.FeatureNameList;
import org.anchoranalysis.io.bean.filepath.provider.FilePathProvider;
import org.anchoranalysis.io.csv.reader.CSVReaderByLine;
import org.anchoranalysis.io.csv.reader.CSVReaderByLine.ReadByLine;
import org.anchoranalysis.io.csv.reader.CSVReaderException;
import org.anchoranalysis.math.statistics.FirstSecondOrderStatistic;
import org.anchoranalysis.plugin.operator.feature.bean.score.ZScore;

public class FeatureListProviderSVMClassifier
        extends FeatureListProviderReferencedFeatures<FeatureInput> {

    private static final String CLASSIFIER_FEATURE_NAME = "svmClassifier";

    // START BEAN PROPERTIES
    @BeanField @Getter @Setter private FilePathProvider filePathProviderSVM;

    /** Normalize features */
    @BeanField @Getter @Setter private boolean normalizeFeatures = true;

    // Multiples the decision value by -1.  Useful for when the feature is used in a
    // minimization/maximization routine.
    @BeanField @Getter @Setter private boolean invertDecisionValue = false;
    // END BEAN PROPERTIES

    @Override
    public FeatureList<FeatureInput> create() throws CreateException {

        try {
            Path fileSVM = filePathProviderSVM.create();

            FeatureList<FeatureInput> features = findModelFeatures(filePathProviderSVM.create());

            return FeatureListFactory.from(buildClassifierFeature(fileSVM, features));

        } catch (OperationFailedException e) {
            throw new CreateException(e);
        }
    }

    private FeatureNameList readFeatureNames(Path filePath) throws IOException {

        FeatureNameList out = new FeatureNameList();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.add(line);
            }
        }

        return out;
    }

    private static List<FirstSecondOrderStatistic> readScale(Path filePath)
            throws CSVReaderException {

        List<FirstSecondOrderStatistic> out = new ArrayList<>();

        try (ReadByLine reader = CSVReaderByLine.open(filePath, " ", false)) {
            reader.read(
                    (line, firstLine) -> {
                        FirstSecondOrderStatistic stat = new FirstSecondOrderStatistic();
                        stat.setMean(Double.parseDouble(line[0]));
                        stat.setScale(Double.parseDouble(line[1]));
                        out.add(stat);
                    });
        }

        return out;
    }

    private static Path differentEnding(Path fileSVM, String ending) {
        String strReplaced = fileSVM.toString().replaceAll("\\.svm$", ending);
        return Paths.get(strReplaced);
    }

    private Feature<FeatureInput> buildClassifierFeature(
            Path fileSVM, FeatureList<FeatureInput> features) throws OperationFailedException {
        try {
            svm_model model = svm.svm_load_model(fileSVM.toString());

            // Assume two-labelling problem, and get the direction we want from the labels
            int[] labels = model.label;
            boolean ascendingLabels = (labels[0] < labels[1]);

            boolean direction = invertDecisionValue ? ascendingLabels : !ascendingLabels;

            FeatureSVMClassifier<FeatureInput> featureClassify =
                    new FeatureSVMClassifier<>(model, features, direction);
            featureClassify.setCustomName(CLASSIFIER_FEATURE_NAME);
            return featureClassify;
        } catch (IOException e) {
            throw new OperationFailedException(e);
        }
    }

    private FeatureList<FeatureInput> findModelFeatures(Path fileSVM)
            throws OperationFailedException {
        try {
            Path filePathFeatures = differentEnding(fileSVM, ".features");
            FeatureNameList featureNames = readFeatureNames(filePathFeatures);

            Path filePathScale = differentEnding(fileSVM, ".scale");
            List<FirstSecondOrderStatistic> listStats = readScale(filePathScale);

            return listFromNames(
                    featureNames, getInitializationParameters().getSharedFeatureSet(), listStats);
        } catch (CSVReaderException | IOException e) {
            throw new OperationFailedException(e);
        }
    }

    private FeatureList<FeatureInput> listFromNames(
            FeatureNameList featureNames,
            NamedProvider<Feature<FeatureInput>> allFeatures,
            List<FirstSecondOrderStatistic> listStats)
            throws OperationFailedException {

        List<String> missing = new ArrayList<>();
        assert (listStats.size() == featureNames.size());

        try {
            FeatureList<FeatureInput> out =
                    FeatureListFactory.mapFromRangeOptional(
                            0,
                            featureNames.size(),
                            NamedProviderGetException.class,
                            index ->
                                    getOrAddToMissing(
                                            featureNames.get(index),
                                            listStats.get(index),
                                            allFeatures,
                                            missing));

            if (!missing.isEmpty()) {
                // Embed exception
                throw MissingFeaturesUtilities.createExceptionForMissingStrings(missing);
            }

            return out;

        } catch (NamedProviderGetException | CreateException e) {
            throw new OperationFailedException(e);
        }
    }

    /**
     * Adds a feature to an out-list if it exists, or adds its name to a missing-list otherwise
     *
     * @throws GetOperationFailedException
     */
    private Optional<Feature<FeatureInput>> getOrAddToMissing(
            String featureName,
            FirstSecondOrderStatistic stat,
            NamedProvider<Feature<FeatureInput>> allFeatures,
            List<String> missing)
            throws NamedProviderGetException {
        Optional<Feature<FeatureInput>> feature = allFeatures.getOptional(featureName);
        if (feature.isPresent()) {
            return Optional.of(maybeNormalise(feature.get(), stat));
        } else {
            missing.add(featureName);
            return Optional.empty();
        }
    }

    private <S extends FeatureInput> Feature<S> maybeNormalise(
            Feature<S> feature, FirstSecondOrderStatistic stat) {
        if (normalizeFeatures) {
            return createScaledFeature(feature, stat);
        } else {
            return feature;
        }
    }

    private <S extends FeatureInput> Feature<S> createScaledFeature(
            Feature<S> feature, FirstSecondOrderStatistic stat) {

        ZScore<S> featureNormalized = new ZScore<>();
        featureNormalized.setItem(feature);
        featureNormalized.setItemMean(new Constant<>("mean", stat.getMean()));
        featureNormalized.setItemStdDev(new Constant<>("stdDev", stat.getScale()));
        featureNormalized.setCustomName(feature.getCustomName() + " (scaled)");
        return featureNormalized;
    }
}
