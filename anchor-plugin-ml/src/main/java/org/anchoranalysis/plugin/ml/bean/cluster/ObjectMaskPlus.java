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
package org.anchoranalysis.plugin.ml.bean.cluster;

import lombok.Getter;
import org.anchoranalysis.core.log.Logger;
import org.anchoranalysis.feature.calculate.FeatureCalculationException;
import org.anchoranalysis.image.core.channel.Channel;
import org.anchoranalysis.image.voxel.object.ObjectMask;
import org.anchoranalysis.plugin.image.intensity.IntensityMeanCalculator;
import org.anchoranalysis.spatial.point.Point3d;
import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * Caches some properties of object-mask, and acts as an input to the clustering algorithm.
 *
 * <p>The properties are:
 *
 * <ul>
 *   <li>center of gravity
 *   <li>mean-intensity value at a particular point
 * </ul>
 */
class ObjectMaskPlus implements Clusterable {

    @Getter private final ObjectMask object;
    private final double[] points;

    public ObjectMaskPlus(ObjectMask object, Channel distanceMap, Logger logger) {
        super();
        this.object = object;

        double distanceFromContour;
        try {
            distanceFromContour =
                    IntensityMeanCalculator.calculateMeanIntensityObject(distanceMap, object);
        } catch (FeatureCalculationException e) {
            logger.errorReporter().recordError(ObjectMaskPlus.class, e);
            distanceFromContour = Double.NaN;
        }

        points = arrayFrom(object.centerOfGravity(), distanceFromContour);
    }

    @Override
    public double[] getPoint() {
        return points;
    }

    private static double[] arrayFrom(Point3d point, double distanceFromContour) {
        double[] arr = new double[4];
        arr[0] = point.x();
        arr[1] = point.y();
        arr[2] = point.z();
        arr[3] = distanceFromContour;
        return arr;
    }
}
