package ch.ethz.biol.cell.imageprocessing.chnl.provider;

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
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeByte;

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
		
		Chnl chnlOut = ChnlFactory.instance().createEmptyInitialised(sd, VoxelDataTypeByte.instance );
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
