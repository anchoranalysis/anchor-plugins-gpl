package org.anchoranalysis.plugin.ml.bean.object.provider;

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

import org.anchoranalysis.core.geometry.Point3d;
import org.anchoranalysis.core.log.Logger;
import org.anchoranalysis.feature.calc.FeatureCalcException;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.plugin.image.intensity.IntensityMeanCalculator;
import org.apache.commons.math3.ml.clustering.Clusterable;

import lombok.Getter;

/** 
 * Caches some properties of  object mask, and acts as an input to the clustering algorithm.
 * <p>
 * The properties are:
 * <ul>
 * <li>center of gravity
 * <li>mean-intensity value at a particular point
 * </ul>
 * 
 * */
class ObjectMaskPlus implements Clusterable {

	@Getter
	private final ObjectMask object;
	private final double[] points;
	
	public ObjectMaskPlus(ObjectMask object, Channel distanceMap, Logger logger ) {
		super();
		this.object = object;
		
		double distanceFromContour;
		try {
			distanceFromContour = IntensityMeanCalculator.calcMeanIntensityObject(distanceMap, object);
		} catch (FeatureCalcException e) {
			logger.errorReporter().recordError(ObjectMaskPlus.class, e);
			distanceFromContour = Double.NaN;
		}
		
		points = arrayFrom(
			object.centerOfGravity(),
			distanceFromContour
		);
	}

	@Override
	public double[] getPoint() {
		return points;
	}
	
	private static double[] arrayFrom( Point3d point, double distanceFromContour ) {
		double[] arr = new double[4];
		arr[0] = point.getX();
		arr[1] = point.getY();
		arr[2] = point.getZ();
		arr[3] = distanceFromContour;
		return arr;
	}
}
