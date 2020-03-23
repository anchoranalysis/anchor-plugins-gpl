package org.anchoranalysis.bean.provider.objs.merge;

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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.core.geometry.Point3d;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.bean.provider.ImageDimProvider;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.extent.ImageRes;
import org.anchoranalysis.image.objmask.ObjMask;
import org.anchoranalysis.image.objmask.ObjMaskCollection;
import org.anchoranalysis.image.objmask.ops.ObjMaskMerger;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.util.MathArrays;


/**
 * Merges objects using the DBScan clustering algorithm based on:
 * 	- Euclidian distance
 *  - an eps (max distance for neighbourhood connection) where distance is less than maxDist (calculated on XY resolution)
 *  
 * @author FEEHANO
 *
 */
public class ObjMaskProviderMergeDBScan extends ObjMaskProviderMergeBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// START BEAN PROPERTIES
	@BeanField
	private ImageDimProvider resProvider;		// provides a resolution
	
	/** A distance map which can also be used for making decisions on merging */
	@BeanField
	private ChnlProvider distanceMapProvider;
	
	/** The maximum distance allowed between center-of-gravities of objects */
	@BeanField
	private UnitValueDistance maxDistCOG;			// provides a maximum distance for a single step (eps). This is resolved in XY plane (assuming isotropy)
	
	/** The maximum distance allowed between the 'distance from contour' values provided from the distanceMap for each point. */
	@BeanField
	private double maxDistDeltaContour = Double.MAX_VALUE;
	// END BEAN PROPERTIES
	
	@Override
	public ObjMaskCollection create() throws CreateException {
		
		ObjMaskCollection objsToMerge = getObjs().create();
				
		try {
			return mergeMultiplex(
				objsToMerge,
				b -> clusterAndMerge(b)
			);
		} catch (OperationFailedException e) {
			throw new CreateException(e);
		}
	}
	
	private static class DistanceCogDistanceMapMeasure implements DistanceMeasure {

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

	
	private ObjMaskCollection clusterAndMerge( ObjMaskCollection objs ) throws OperationFailedException {
		
		ImageRes res = MergeHelpUtilities.calcRes(resProvider);
	
		DBSCANClusterer<ObjMaskWithCOG> clusterer = new DBSCANClusterer<ObjMaskWithCOG>(
			1.0,	// Maximum distance allowed to merge points
			0,	// Ensures no object is discarded as "noise"
			new DistanceCogDistanceMapMeasure(res, maxDistCOG, maxDistDeltaContour)
		);
		
		try {
			Chnl distanceMap = distanceMapProvider.create();
					
			List<Cluster<ObjMaskWithCOG>> clusters = clusterer.cluster( convert(objs, distanceMap) );
			return mergeClusters(clusters);
			
		} catch (CreateException e) {
			throw new OperationFailedException(e);
		}
	
	}
	
	private static ObjMaskCollection mergeClusters(
		List<Cluster<ObjMaskWithCOG>> clusters
	) throws OperationFailedException {

		ObjMaskCollection out = new ObjMaskCollection();
		for( Cluster<ObjMaskWithCOG> c : clusters) {
			// Merge objects together
			out.add( mergeCluster(c) );
		}
		return out;
	}
	
	private static ObjMask mergeCluster( Cluster<ObjMaskWithCOG> cluster ) throws OperationFailedException {
		return ObjMaskMerger.merge( convert(cluster.getPoints()) );
	}
	
	private static ObjMaskCollection convert( Collection<ObjMaskWithCOG> objs ) {
		return new ObjMaskCollection(
			objs.stream().map( c -> c.om ).collect( Collectors.toList() )
		);
	}
	
	private Collection<ObjMaskWithCOG> convert( ObjMaskCollection objs, 	Chnl distanceMap ) {
		return objs.asList().stream().map( c ->
			new ObjMaskWithCOG(c, distanceMap, getLogger()  )
		).collect( Collectors.toList() );
	}

	public ChnlProvider getDistanceMapProvider() {
		return distanceMapProvider;
	}

	public void setDistanceMapProvider(ChnlProvider distanceMapProvider) {
		this.distanceMapProvider = distanceMapProvider;
	}

	public UnitValueDistance getMaxDistCOG() {
		return maxDistCOG;
	}

	public void setMaxDistCOG(UnitValueDistance maxDistCOG) {
		this.maxDistCOG = maxDistCOG;
	}

	public double getMaxDistDeltaContour() {
		return maxDistDeltaContour;
	}

	public void setMaxDistDeltaContour(double maxDistDeltaContour) {
		this.maxDistDeltaContour = maxDistDeltaContour;
	}

	public ImageDimProvider getResProvider() {
		return resProvider;
	}

	public void setResProvider(ImageDimProvider resProvider) {
		this.resProvider = resProvider;
	}
}
