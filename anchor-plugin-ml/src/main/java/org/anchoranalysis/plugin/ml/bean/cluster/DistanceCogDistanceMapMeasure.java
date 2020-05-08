package org.anchoranalysis.plugin.ml.bean.cluster;

/*-
 * #%L
 * anchor-plugin-ml
 * %%
 * Copyright (C) 2016 - 2020 ETH Zurich, University of Zurich, Owen Feehan
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

import java.util.Arrays;

import org.anchoranalysis.core.geometry.Point3d;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.extent.ImageRes;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.util.MathArrays;

class DistanceCogDistanceMapMeasure implements DistanceMeasure {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ImageRes res;
	private UnitValueDistance maxDist;
	private double maxDistDeltaContour;
			
	public DistanceCogDistanceMapMeasure(ImageRes res, UnitValueDistance maxDist, double maxDistDeltaContour) {
		super();
		this.res = res;
		this.maxDist = maxDist;
		this.maxDistDeltaContour = maxDistDeltaContour;
	}

	@Override
	public double compute(double[] a, double[] b) throws DimensionMismatchException {
		
		// The first three indices are the 3D cog
		// The fourth index is the distance-map value (i.e. distance from the contour)
		double distCOG = normalisedDistanceCOG(a,b);
		
		// The difference in distances-from-the-countour (from the distance map) between the two points
		double distDeltaDistanceContour = normalisedDistanceDeltaContour(a, b);
		
		return Math.max(distCOG, distDeltaDistanceContour);
	}
			
	private double normalisedDistanceDeltaContour( double[] a, double[] b  ) {
		double dist = Math.abs( 
			extractContourDistance(a) - extractContourDistance(b)		
		);
		return dist/maxDistDeltaContour;
	}
	
	
	/** Returns a distance value that is <= 1 if the distance between the COGs is less than maxRslv, 
	 *   or greater than that otherwise
	 *   
	 * @param a
	 * @param b
	 * @return
	 */
	private double normalisedDistanceCOG( double[] a, double[] b ) {
		
		// Maximum distance when measured in voxels along the vector between our points 
		double maxDistVoxels = maxDist.rslv(res, convert(a), convert(b) );
		
		// We measure the voxel distance between the points
		double distVoxels = MathArrays.distance( extractPnt(a), extractPnt(b) );
		
		//System.out.printf("maxDistVox=%f  distVoxels=%f%n", maxDistVoxels, distVoxels);
		
		return distVoxels/maxDistVoxels;
	}
	
	private static double[] extractPnt( double[] pnt ) {
		return Arrays.copyOfRange(pnt, 0, 3);
	}
	
	private static double extractContourDistance( double[] pnt ) {
		return pnt[3];
	}
			
	/** Converts a double-array (first three elements) to a 3d point */
	private static Point3d convert( double arr[] ) {
		return new Point3d( arr[0], arr[1], arr[2] );
	}
}
