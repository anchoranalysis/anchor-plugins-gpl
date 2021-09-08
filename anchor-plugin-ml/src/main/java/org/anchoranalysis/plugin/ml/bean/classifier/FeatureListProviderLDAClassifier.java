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

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.shared.dictionary.DictionaryProvider;
import org.anchoranalysis.bean.shared.relation.GreaterThanBean;
import org.anchoranalysis.bean.shared.relation.threshold.RelationToConstant;
import org.anchoranalysis.bean.xml.exception.ProvisionFailedException;
import org.anchoranalysis.core.identifier.provider.NamedProviderGetException;
import org.anchoranalysis.core.value.Dictionary;
import org.anchoranalysis.feature.bean.Feature;
import org.anchoranalysis.feature.bean.list.FeatureList;
import org.anchoranalysis.feature.bean.list.FeatureListFactory;
import org.anchoranalysis.feature.bean.list.ReferencedFeatures;
import org.anchoranalysis.feature.bean.operator.Constant;
import org.anchoranalysis.feature.bean.operator.Reference;
import org.anchoranalysis.feature.bean.operator.Sum;
import org.anchoranalysis.feature.input.FeatureInput;
import org.anchoranalysis.plugin.operator.feature.bean.arithmetic.MultiplyByConstant;
import org.anchoranalysis.plugin.operator.feature.bean.conditional.IfCondition;

public class FeatureListProviderLDAClassifier<T extends FeatureInput>
        extends ReferencedFeatures<T> {

    private static final String LDA_THRESHOLD_KEY = "__ldaThreshold";

    private static final String FEATURE_NAME_SCORE = "ldaScore";
    private static final String FEATURE_NAME_THRESHOLD = "ldaThreshold";
    private static final String FEATURE_NAME_CLASSIFIER = "ldaClassifier";

    // START BEAN PROPERTIES
    @BeanField @Getter @Setter private DictionaryProvider dictionary;
    // END BEAN PROPERTIES

    @Override
    public FeatureList<T> get() throws ProvisionFailedException {

        Dictionary dictionaryCreated = dictionary.get();

        checkForMissingFeatures(dictionaryCreated);

        double threshold = dictionaryCreated.getAsDouble(LDA_THRESHOLD_KEY);

        return FeatureListFactory.from(
                createScoreFeature(dictionaryCreated),
                createThresholdFeature(threshold),
                createClassifierFeature(threshold));
    }

    private void checkForMissingFeatures(Dictionary dictionary) throws ProvisionFailedException {

        List<String> list = new ArrayList<>();

        // For now let's just check all the feature are present
        for (String name : dictionary.keys()) {

            // Skip the threshold name
            if (name.equals(LDA_THRESHOLD_KEY)) {
                continue;
            }

            if (getInitialization().getSharedFeatures().keys().contains(name)) {
                list.add(name);
            }
        }

        if (!list.isEmpty()) {
            throw MissingFeaturesUtilities.createExceptionForMissingStrings(list);
        }
    }

    private Feature<T> createScoreFeature(Dictionary dictionary) throws ProvisionFailedException {

        Sum<T> sum = new Sum<>();
        sum.setIgnoreNaN(true);

        try {
            // For now let's just check all the feature are present
            for (String name : dictionary.keys()) {

                // Skip the threshold name
                if (name.equals(LDA_THRESHOLD_KEY)) {
                    continue;
                }

                Feature<T> feature =
                        getInitialization().getSharedFeatures().getException(name).downcast();
                sum.getList().add(new MultiplyByConstant<>(feature, dictionary.getAsDouble(name)));
            }

        } catch (NamedProviderGetException e) {
            throw new ProvisionFailedException(e);
        }

        sum.setCustomName(FEATURE_NAME_SCORE);
        return sum;
    }

    private Feature<T> createThresholdFeature(double threshold) {
        return new Constant<>(FEATURE_NAME_THRESHOLD, threshold);
    }

    private Feature<T> createClassifierFeature(double threshold) {
        // If we are below the the threshold we output -1

        Reference<T> score = new Reference<>(FEATURE_NAME_SCORE);

        IfCondition<T> featureThreshold = new IfCondition<>();
        featureThreshold.setFeatureCondition(score);
        featureThreshold.setItem(new Constant<>(1));
        featureThreshold.setFeatureElse(new Constant<>(0));
        featureThreshold.setThreshold(new RelationToConstant(new GreaterThanBean(), threshold));

        featureThreshold.setCustomName(FEATURE_NAME_CLASSIFIER);
        return featureThreshold;
    }
}
