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

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.anchoranalysis.core.exception.CreateException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class MissingFeaturesUtilities {

    public static CreateException createExceptionForMissingStrings(List<String> listNames) {
        // Then we have at least one missing feature, throw an exception
        StringBuilder sb = new StringBuilder();

        sb.append("The following features referenced in the model are missing:");
        sb.append(System.lineSeparator());

        for (String featureName : listNames) {
            sb.append(featureName);
            sb.append(System.lineSeparator());
        }

        return new CreateException(sb.toString());
    }
}
