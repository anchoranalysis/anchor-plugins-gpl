package org.anchoranalysis.plugin.ml.bean.cluster;

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
import org.anchoranalysis.core.log.LogErrorReporter;
import org.anchoranalysis.feature.calc.FeatureCalcException;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.objectmask.ObjectMask;
import org.anchoranalysis.plugin.image.intensity.IntensityMeanCalculator;
import org.apache.commons.math3.ml.clustering.Clusterable;

/** 
 * Caches some properties of  object mask, and acts as an input to the clustering algorithm. The properties are:
 *   1. center of gravity
 *   2. mean-intensity value at a particular point
 * 
 * */
class ObjMaskWithCOG implements Clusterable {

	private ObjectMask om;
	private double[] pnts;
	
	public ObjMaskWithCOG(ObjectMask om, Channel distanceMap, LogErrorReporter logErrorReporter ) {
		super();
		this.om = om;
		
		Point3d cogD = om.centerOfGravity();
		
		double distanceFromContour;
		try {
			distanceFromContour = IntensityMeanCalculator.calcMeanIntensityObjMask(distanceMap, om);
		} catch (FeatureCalcException e) {
			logErrorReporter.getErrorReporter().recordError(ObjMaskWithCOG.class, e);
			distanceFromContour = Double.NaN;
		}
		
		pnts = arrayFrom(cogD , distanceFromContour );
	}

	@Override
	public double[] getPoint() {
		return pnts;
	}
	
	private static double[] arrayFrom( Point3d pnt, double distanceFromContour ) {
		double[] arr = new double[4];
		arr[0] = pnt.getX();
		arr[1] = pnt.getY();
		arr[2] = pnt.getZ();
		arr[3] = distanceFromContour;
		return arr;
	}

	public ObjectMask getObjectMask() {
		return om;
	}
}
