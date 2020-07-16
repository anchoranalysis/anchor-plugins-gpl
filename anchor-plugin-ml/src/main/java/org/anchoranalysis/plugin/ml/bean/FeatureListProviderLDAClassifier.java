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

import java.util.ArrayList;
import java.util.List;
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

    // START BEAN PROPERTIES
    @BeanField private KeyValueParamsProvider keyValueParamsProvider;
    // END BEAN PROPERTIES

    private static String ldaThresholdKey = "__ldaThreshold";

    private static String featureNameScore = "ldaScore";
    private static String featureNameThreshold = "ldaThreshold";
    private static String featureNameClassifier = "ldaClassifier";

    @Override
    public FeatureList<T> create() throws CreateException {

        KeyValueParams kpv = keyValueParamsProvider.create();

        if (kpv == null) {
            throw new CreateException("Cannot find KeyValueParams for LDA Model");
        }

        checkForMissingFeatures(kpv);

        double threshold = kpv.getPropertyAsDouble(ldaThresholdKey);

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
            if (name.equals(ldaThresholdKey)) {
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
                if (name.equals(ldaThresholdKey)) {
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

        sum.setCustomName(featureNameScore);
        return sum;
    }

    private Feature<T> createThresholdFeature(double threshold) {
        return new Constant<>(featureNameThreshold, threshold);
    }

    private Feature<T> createClassifierFeature(double threshold) {
        // If we are below the the threshold we output -1

        Reference<T> score = new Reference<>(featureNameScore);

        IfCondition<T> featThresh = new IfCondition<>();
        featThresh.setFeatureCondition(score);
        featThresh.setItem(new Constant<>(1));
        featThresh.setFeatureElse(new Constant<>(0));
        featThresh.setThreshold(new RelationToConstant(new GreaterThanBean(), threshold));

        featThresh.setCustomName(featureNameClassifier);
        return featThresh;
    }

    public KeyValueParamsProvider getKeyValueParamsProvider() {
        return keyValueParamsProvider;
    }

    public void setKeyValueParamsProvider(KeyValueParamsProvider keyValueParamsProvider) {
        this.keyValueParamsProvider = keyValueParamsProvider;
    }
}
