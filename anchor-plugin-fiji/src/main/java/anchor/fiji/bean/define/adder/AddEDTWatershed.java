package anchor.fiji.bean.define.adder;

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

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.define.Define;
import org.anchoranalysis.bean.define.adder.DefineAdderWithPrefixBean;
import org.anchoranalysis.bean.xml.error.BeanXmlException;
import org.anchoranalysis.image.bean.provider.BinaryChnlProvider;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.bean.provider.ImageDimProvider;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.bean.unitvalue.volume.UnitValueVolume;
import org.anchoranalysis.image.bean.unitvalue.volume.UnitValueVolumeVoxels;
import org.anchoranalysis.plugin.image.bean.blur.BlurGaussian3D;
import org.anchoranalysis.plugin.image.bean.blur.BlurStrategy;

import ch.ethz.biol.cell.imageprocessing.binaryimgchnl.provider.BinaryChnlProviderReference;
import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderDuplicate;
import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderBlur;
import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderReference;
import ch.ethz.biol.cell.imageprocessing.objmask.provider.ObjMaskProviderReference;

import static anchor.fiji.bean.define.adder.FactoryOther.*;
import static anchor.fiji.bean.define.adder.FactorySgmn.*;

/**
 * Performs a Watershed on an EDT transform by bundling together several other beans
 * 
 * This is used to avoid repetitive bean-definitions in Define, but while still providing
 *  visualization of all the intermediate steps that occur during the transformation, which
 *  are typically vital for visualization.
 *  
 *  For now, it only works in 2D, but can be easily extended for 3D.
 *  
 *  The steps are:
 *  	1. Find connected components of a binary mask
 *  	2. Find the distance transform of the above
 *  	3. Invert the distance transform
 *  	4. Find minima points from the above
 *  	5. Merge minima points that occur within a certain distance (they become the same object, but it can be disconnected voxelwise)
 *  	6. Create seeds by drawing a line between the merged-minima-points so they are now connected.
 *  	7. Apply the watershed transformation using the seeds to create segments
 * 
 * @author Owen Feehan
 *
 */
public class AddEDTWatershed extends DefineAdderWithPrefixBean {

	private static final String CONNECTED_INPUT = "objsInputConnected";
	private static final String DISTANCE_TRANSFORM = "chnlDistance";
	private static final String DISTANCE_TRANSFORM_SMOOTH = "chnlDistanceSmooth";
	private static final String DISTANCE_TRANSFORM_BEFORE_INVERT = "chnlDistanceBeforeInvert";
	private static final String MINIMA_UNMERGED = "objsMinimaUnmerged";	// Minima have not yet been 'merged' together
	private static final String MINIMA_MERGED = "objsMinimaMerged";		// Minima are merged, but can still be disconnected
	private static final String SEEDS = "objsSeeds";
	private static final String SEGMENTS = "objsSegments";
	
	// START BEAN PROPERTIES
	/** The ID of the binary input mask that determines the region of the watershed */
	@BeanField
	private String binaryInputChnlID;
	
	@BeanField
	private UnitValueVolume minVolumeConnectedComponent = new UnitValueVolumeVoxels(1);
	
	/** Multiplies the distance transform by this factor to make it more meaningful in a short */
	@BeanField
	private double distanceTransformMultiplyBy = 1.0;

	/** If non-zero, a Gaussian blur is applied to the distance transform using the sigma in meters below */
	@BeanField
	private double distanceTransformSmoothSigmaMeters = 0;
	
	@BeanField
	private UnitValueDistance maxDistanceBetweenMinima;
	
	/** The maximum distance allowed between two seeds in terms of their values in the distance map */
	@BeanField
	private double maxDistanceDeltaContour = Double.MAX_VALUE;
	// END BEAN PROPERTIES
	
	@Override
	public void addTo(Define define) throws BeanXmlException {

		// Step 1
		addConnectedInput(define);
		
		// Steps 2-3
		addDistanceTransform(define);
		
		// Steps 4-6
		addSeedFinding( define );
		
		// Step 7
		addSegments(define);

	}
	
	private void addSegments(Define define) throws BeanXmlException {
		addWithName(define, SEGMENTS,
			watershedSegment( objs(CONNECTED_INPUT), objs(SEEDS), chnl(DISTANCE_TRANSFORM) )
		);
	}
	
	private void addConnectedInput(Define define) throws BeanXmlException {
		addWithName(define, CONNECTED_INPUT,
			connectedComponentsInput(inputMask(),minVolumeConnectedComponent)
		);
	}
	
	private void addDistanceTransform(Define define) throws BeanXmlException {
		
		addWithName(define, DISTANCE_TRANSFORM_BEFORE_INVERT,
			distanceTransformBeforeInvert(inputMask(), distanceTransformMultiplyBy )
		);
		
		if (isDistanceTransformSmoothed()) {
			addWithName(define, DISTANCE_TRANSFORM_SMOOTH,
				smooth( duplicateChnl(DISTANCE_TRANSFORM_BEFORE_INVERT), distanceTransformSmoothSigmaMeters )
			);
		}
		
		addWithName(define, DISTANCE_TRANSFORM,
			distanceTransformAfterInvert( duplicateChnl(srcForInversion()) )
		);
	}
	
	private boolean isDistanceTransformSmoothed() {
		return distanceTransformSmoothSigmaMeters > 0;
	}
	
	private String srcForInversion() {
		if (isDistanceTransformSmoothed()) {
			return DISTANCE_TRANSFORM_SMOOTH;
		} else {
			return DISTANCE_TRANSFORM_BEFORE_INVERT;
		}
	}
	
	private static ChnlProvider smooth( ChnlProvider src, double distanceTransformSmoothedSigmaMeters ) {
		ChnlProviderBlur provider = new ChnlProviderBlur();
		provider.setStrategy(
			createBlurStrategy(distanceTransformSmoothedSigmaMeters)		
		);
		provider.setChnl( src );
		return provider;
	}
	
	private static BlurStrategy createBlurStrategy( double distanceTransformSmoothedSigmaMeters ) {
		BlurGaussian3D blurStrategy = new BlurGaussian3D();
		blurStrategy.setSigma( distanceTransformSmoothedSigmaMeters );
		blurStrategy.setSigmaInMeters(true);
		return blurStrategy;
	}
	
	private void addSeedFinding(Define define) throws BeanXmlException {
		addWithName(define, MINIMA_UNMERGED,
			minimaUnmerged( inputMask(), chnl(DISTANCE_TRANSFORM) )
		);
		
		addWithName(define, MINIMA_MERGED,
			mergeMinima(
				objs(MINIMA_UNMERGED),
				objs(CONNECTED_INPUT),
				dimensions(),
				chnl(DISTANCE_TRANSFORM),
				maxDistanceBetweenMinima,
				maxDistanceDeltaContour
			)
		);
		
		addWithName(define, SEEDS,
			seeds(
				objs(MINIMA_MERGED),
				dimensions()
			)
		);
	}
		
	private ChnlProvider chnl( String unresolvedID ) {
		return new ChnlProviderReference( rslvName(unresolvedID) );		
	}
	
	private ImageDimProvider dimensions() {
		return dimsFromChnl(
			chnl(DISTANCE_TRANSFORM)
		);
	}
	
	private ChnlProvider duplicateChnl( String unresolvedID ) {
		ChnlProviderDuplicate dup = new ChnlProviderDuplicate();
		dup.setChnl( chnl(unresolvedID) );
		return dup;
	}
	
	private ObjectCollectionProvider objs( String unresolvedID ) {
		return new ObjMaskProviderReference( rslvName(unresolvedID) );		
	}
	
	private BinaryChnlProvider inputMask() {
		return new BinaryChnlProviderReference(binaryInputChnlID);
	}
	

	public String getBinaryInputChnlID() {
		return binaryInputChnlID;
	}

	public void setBinaryInputChnlID(String binaryInputChnlID) {
		this.binaryInputChnlID = binaryInputChnlID;
	}

	public UnitValueVolume getMinVolumeConnectedComponent() {
		return minVolumeConnectedComponent;
	}

	public void setMinVolumeConnectedComponent(UnitValueVolume minVolumeConnectedComponent) {
		this.minVolumeConnectedComponent = minVolumeConnectedComponent;
	}

	public double getDistanceTransformMultiplyBy() {
		return distanceTransformMultiplyBy;
	}

	public void setDistanceTransformMultiplyBy(double distanceTransformMultiplyBy) {
		this.distanceTransformMultiplyBy = distanceTransformMultiplyBy;
	}

	public UnitValueDistance getMaxDistanceBetweenMinima() {
		return maxDistanceBetweenMinima;
	}

	public void setMaxDistanceBetweenMinima(UnitValueDistance maxDistanceBetweenMinima) {
		this.maxDistanceBetweenMinima = maxDistanceBetweenMinima;
	}

	public double getDistanceTransformSmoothSigmaMeters() {
		return distanceTransformSmoothSigmaMeters;
	}

	public void setDistanceTransformSmoothSigmaMeters(double distanceTransformSmoothSigmaMeters) {
		this.distanceTransformSmoothSigmaMeters = distanceTransformSmoothSigmaMeters;
	}

	public double getMaxDistanceDeltaContour() {
		return maxDistanceDeltaContour;
	}

	public void setMaxDistanceDeltaContour(double maxDistanceDeltaContour) {
		this.maxDistanceDeltaContour = maxDistanceDeltaContour;
	}
}
