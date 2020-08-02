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
package org.anchoranalysis.plugin.ml.bean.object.provider;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.core.error.friendly.AnchorFriendlyRuntimeException;
import org.anchoranalysis.core.geometry.Point3d;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.extent.ImageResolution;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.util.MathArrays;

@AllArgsConstructor
class DistanceCogDistanceMapMeasure implements DistanceMeasure {

    /** */
    private static final long serialVersionUID = 1L;

    private final ImageResolution res;
    private final UnitValueDistance maxDistance;
    private final double maxDistanceDeltaContour;

    @Override
    public double compute(double[] a, double[] b) {

        try {
            // The first three indices are the 3D cog
            // The fourth index is the distance-map value (i.e. distance from the contour)
            double distanceCOG = normalisedDistanceCOG(a, b);

            // The difference in distances-from-the-countour (from the distance map) between the two
            // points
            double distanceDeltaDistanceContour = normalisedDistanceDeltaContour(a, b);

            return Math.max(distanceCOG, distanceDeltaDistanceContour);
        } catch (OperationFailedException e) {
            throw new AnchorFriendlyRuntimeException(
                    "An exception occurred calculating distances", e);
        }
    }

    private double normalisedDistanceDeltaContour(double[] a, double[] b) {
        double distance = Math.abs(extractContourDistance(a) - extractContourDistance(b));
        return distance / maxDistanceDeltaContour;
    }

    /**
     * Returns a distance value that is <= 1 if the distance between the COGs is less than resolved
     * max-distance, or greater than that otherwise
     *
     * @param a
     * @param b
     * @return
     * @throws OperationFailedException
     */
    private double normalisedDistanceCOG(double[] a, double[] b) throws OperationFailedException {

        // Maximum distance when measured in voxels along the vector between our points
        double maxDistanceVoxels = maxDistance.resolve(Optional.of(res), convert(a), convert(b));

        // We measure the voxel distance between the points
        double distanceVoxels = MathArrays.distance(extractPoint(a), extractPoint(b));
        return distanceVoxels / maxDistanceVoxels;
    }

    private static double[] extractPoint(double[] point) {
        return Arrays.copyOfRange(point, 0, 3);
    }

    private static double extractContourDistance(double[] point) {
        return point[3];
    }

    /** Converts a double-array (first three elements) to a 3d point */
    private static Point3d convert(double[] arr) {
        return new Point3d(arr[0], arr[1], arr[2]);
    }
}
