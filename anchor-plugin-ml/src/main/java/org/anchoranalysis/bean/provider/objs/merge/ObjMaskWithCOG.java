package org.anchoranalysis.bean.provider.objs.merge;

/*
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2019 F. Hoffmann-La Roche Ltd
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

import org.anchoranalysis.core.geometry.Point3d;
import org.anchoranalysis.core.log.LogErrorReporter;
import org.anchoranalysis.feature.calc.FeatureCalcException;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.objmask.ObjMask;
import org.apache.commons.math3.ml.clustering.Clusterable;

import ch.ethz.biol.cell.mpp.nrg.feature.objmask.IntensityMean;

/** Caches some properties of  object mask, and acts as an input to the clustering algorithm. The properties are:
 *   1. center of gravity
 *   2. mean-intensity value at a particular point
 * 
 * */
class ObjMaskWithCOG implements Clusterable {

	ObjMask om;
	private double[] pnts;
	
	public ObjMaskWithCOG(ObjMask om, Chnl distanceMap, LogErrorReporter logErrorReporter ) {
		super();
		this.om = om;
		
		Point3d cogD = om.centerOfGravity();
		
		double distanceFromContour;
		try {
			distanceFromContour = IntensityMean.calcMeanIntensityObjMask(distanceMap, om);
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
}