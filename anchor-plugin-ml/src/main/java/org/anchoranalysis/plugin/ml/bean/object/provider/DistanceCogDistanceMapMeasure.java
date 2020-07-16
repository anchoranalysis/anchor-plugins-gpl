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
