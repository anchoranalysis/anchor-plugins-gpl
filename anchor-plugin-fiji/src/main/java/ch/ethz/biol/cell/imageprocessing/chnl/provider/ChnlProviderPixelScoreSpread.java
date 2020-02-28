package ch.ethz.biol.cell.imageprocessing.chnl.provider;

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


import java.util.List;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.feature.bean.Feature;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.bean.provider.ObjMaskProvider;
import org.anchoranalysis.image.bean.sgmn.objmask.ObjMaskSgmn;
import org.anchoranalysis.image.binary.BinaryChnl;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.chnl.factory.ChnlFactory;
import org.anchoranalysis.image.extent.ImageDim;
import org.anchoranalysis.image.objmask.ObjMaskCollection;
import org.anchoranalysis.image.objmask.match.ObjWithMatches;
import org.anchoranalysis.image.voxel.box.VoxelBoxList;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;

import ch.ethz.biol.cell.imageprocessing.binaryimgchnl.provider.BinaryImgChnlProviderFromObjMasks;
import ch.ethz.biol.cell.imageprocessing.objmask.provider.ObjMaskProviderSeededObjSgmn;
import ch.ethz.biol.cell.sgmn.objmask.watershed.yeong.ObjMaskSgmnWatershedYeong;

// NOT USED, NOT FINISHED, Let's keep for now in case the methods are useful
public class ChnlProviderPixelScoreSpread extends ChnlProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// START BEAN PROPERTIES
	@BeanField
	private ChnlProvider intensityProvider;
	
	@BeanField
	private Feature pixelScore;
	
	@BeanField
	private ObjMaskProvider objMaskProviderSource;
	
	@BeanField
	private ObjMaskProvider objMaskProviderTarget;
	
	@BeanField
	private ObjMaskSgmn sgmnSpread = new ObjMaskSgmnWatershedYeong();
	// END BEAN PROPERTIES
	
	@Override
	public Chnl create() throws CreateException {

		Chnl chnlIntensity = intensityProvider.create();
		
		ObjMaskCollection objsSrc = objMaskProviderSource.create();
		
		ObjMaskCollection objsTrgt = objMaskProviderTarget.create();
		
		ImageDim sd = chnlIntensity.getDimensions();
		
		// We grow the objsSrc to objsTrgt and create a one-to-one mapping between the two
		List<ObjWithMatches> matches = createMappingToGrownSeeds( objsSrc, objsTrgt, sd );
		
		
		VoxelBoxList voxelBoxList = new VoxelBoxList();
		voxelBoxList.add(chnlIntensity.getVoxelBox());
		
		// We calculate the score for 
		
		Chnl chnlOut = ChnlFactory.instance().createEmptyInitialised(sd, VoxelDataTypeUnsignedByte.instance );
		int i = 0;
		for( ObjWithMatches owm : matches ) {
			chnlOut.getVoxelBox().any().setPixelsCheckMask( owm.getMatches().get(0), i++);
		}
		return chnlOut;
		
	}
	
	private List<ObjWithMatches> createMappingToGrownSeeds(
		ObjMaskCollection objsSrc,
		ObjMaskCollection objsTrgt,
		ImageDim dim
	) throws CreateException {
		BinaryChnl outsideSrc = BinaryImgChnlProviderFromObjMasks.create( objsSrc, dim, true);
		
		// Create a distance map for the area outside objsSrc.... we will grow along this intensity
		Chnl distanceMap = ChnlProviderDistanceTransformExact3D.createDistanceMapForChnl(
			outsideSrc,
			false,
			1,
			1,
			true,
			false
		);
		
		// Perform a watershed this distance-map, to grow objsSrc into objsTrgt
		ObjMaskCollection objsSpread = ObjMaskProviderSeededObjSgmn.createWithSourceObjs(
			distanceMap,
			objsSrc,
			objsTrgt,
			sgmnSpread
		);
		
		// As we are not sure about the order of objsSpread that returns, we intersect them with our seeds (objsSrc)
		//   to create a mapping
		return ChnlProviderAssignFromIntersectingObjects.matchIntersectingObjectsSingle(objsSrc, objsSpread);		
	}
	
	
	public ChnlProvider getIntensityProvider() {
		return intensityProvider;
	}

	public void setIntensityProvider(ChnlProvider intensityProvider) {
		this.intensityProvider = intensityProvider;
	}

	

	public Feature getPixelScore() {
		return pixelScore;
	}

	public void setPixelScore(Feature pixelScore) {
		this.pixelScore = pixelScore;
	}

	public ObjMaskProvider getObjMaskProviderSource() {
		return objMaskProviderSource;
	}

	public void setObjMaskProviderSource(ObjMaskProvider objMaskProviderSource) {
		this.objMaskProviderSource = objMaskProviderSource;
	}

	public ObjMaskProvider getObjMaskProviderTarget() {
		return objMaskProviderTarget;
	}

	public void setObjMaskProviderTarget(ObjMaskProvider objMaskProviderTarget) {
		this.objMaskProviderTarget = objMaskProviderTarget;
	}

	public ObjMaskSgmn getSgmnSpread() {
		return sgmnSpread;
	}

	public void setSgmnSpread(ObjMaskSgmn sgmnSpread) {
		this.sgmnSpread = sgmnSpread;
	}
}
