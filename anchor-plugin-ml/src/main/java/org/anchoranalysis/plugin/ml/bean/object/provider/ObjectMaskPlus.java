package org.anchoranalysis.plugin.ml.bean.object.provider;

import lombok.Getter;
import org.anchoranalysis.core.geometry.Point3d;
import org.anchoranalysis.core.log.Logger;
import org.anchoranalysis.feature.calc.FeatureCalcException;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.plugin.image.intensity.IntensityMeanCalculator;
import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * Caches some properties of object mask, and acts as an input to the clustering algorithm.
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
                    IntensityMeanCalculator.calcMeanIntensityObject(distanceMap, object);
        } catch (FeatureCalcException e) {
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
        arr[0] = point.getX();
        arr[1] = point.getY();
        arr[2] = point.getZ();
        arr[3] = distanceFromContour;
        return arr;
    }
}
