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

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.shared.params.keyvalue.KeyValueParamsProvider;
import org.anchoranalysis.bean.shared.relation.GreaterThanBean;
import org.anchoranalysis.bean.shared.relation.threshold.RelationToConstant;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.core.name.provider.NamedProviderGetException;
import org.anchoranalysis.core.params.KeyValueParams;
import org.anchoranalysis.feature.bean.Feature;
import org.anchoranalysis.feature.bean.list.FeatureList;
import org.anchoranalysis.feature.bean.list.FeatureListFactory;
import org.anchoranalysis.feature.bean.list.FeatureListProviderReferencedFeatures;
import org.anchoranalysis.feature.bean.operator.Constant;
import org.anchoranalysis.feature.bean.operator.Reference;
import org.anchoranalysis.feature.bean.operator.Sum;
import org.anchoranalysis.feature.input.FeatureInput;
import org.anchoranalysis.plugin.operator.feature.bean.arithmetic.MultiplyByConstant;
import org.anchoranalysis.plugin.operator.feature.bean.conditional.IfCondition;

public class FeatureListProviderLDAClassifier<T extends FeatureInput>
        extends FeatureListProviderReferencedFeatures<T> {

    private static final String LDA_THRESHOLD_KEY = "__ldaThreshold";

    private static final String FEATURE_NAME_SCORE = "ldaScore";
    private static final String FEATURE_NAME_THRESHOLD = "ldaThreshold";
    private static final String FEATURE_NAME_CLASSIFIER = "ldaClassifier";

    // START BEAN PROPERTIES
    @BeanField @Getter @Setter private KeyValueParamsProvider params;
    // END BEAN PROPERTIES

    @Override
    public FeatureList<T> create() throws CreateException {

        KeyValueParams kpv = params.create();

        if (kpv == null) {
            throw new CreateException("Cannot find KeyValueParams for LDA Model");
        }

        checkForMissingFeatures(kpv);

        double threshold = kpv.getPropertyAsDouble(LDA_THRESHOLD_KEY);

        return FeatureListFactory.from(
                createScoreFeature(kpv),
                createThresholdFeature(threshold),
                createClassifierFeature(threshold));
    }

    private void checkForMissingFeatures(KeyValueParams kpv) throws CreateException {

        List<String> list = new ArrayList<>();

        // For now let's just check all the feature are present
        for (String name : kpv.keySet()) {

            // Skip the threshold name
            if (name.equals(LDA_THRESHOLD_KEY)) {
                continue;
            }

            if (getInitializationParameters().getSharedFeatureSet().keys().contains(name)) {
                list.add(name);
            }
        }

        if (!list.isEmpty()) {
            throw MissingFeaturesUtilities.createExceptionForMissingStrings(list);
        }
    }

    private Feature<T> createScoreFeature(KeyValueParams kpv) throws CreateException {

        Sum<T> sum = new Sum<>();
        sum.setIgnoreNaN(true);

        try {
            // For now let's just check all the feature are present
            for (String name : kpv.keySet()) {

                // Skip the threshold name
                if (name.equals(LDA_THRESHOLD_KEY)) {
                    continue;
                }

                Feature<T> feature =
                        getInitializationParameters()
                                .getSharedFeatureSet()
                                .getException(name)
                                .downcast();
                sum.getList().add(new MultiplyByConstant<>(feature, kpv.getPropertyAsDouble(name)));
            }

        } catch (NamedProviderGetException e) {
            throw new CreateException(e);
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

        IfCondition<T> featThresh = new IfCondition<>();
        featThresh.setFeatureCondition(score);
        featThresh.setItem(new Constant<>(1));
        featThresh.setFeatureElse(new Constant<>(0));
        featThresh.setThreshold(new RelationToConstant(new GreaterThanBean(), threshold));

        featThresh.setCustomName(FEATURE_NAME_CLASSIFIER);
        return featThresh;
    }
}
