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


import java.nio.ByteBuffer;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.bean.provider.ImageDimProvider;
import org.anchoranalysis.image.bean.provider.ObjMaskProvider;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBox;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.chnl.factory.ChnlFactory;
import org.anchoranalysis.image.extent.BoundingBox;
import org.anchoranalysis.image.objmask.ObjMask;
import org.anchoranalysis.image.objmask.ObjMaskCollection;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeByte;


// Euclidian distance transform from ImageJ
//
// Does not re-use the binary image
public class ChnlProviderDistanceTransformFromObjsExact3D extends ChnlProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// START PROPERTIES
	@BeanField
	private ObjMaskProvider objMaskProvider;
	
	@BeanField
	private ImageDimProvider dimProvider;
	
	@BeanField
	private boolean suppressZ = false;
	
	@BeanField
	private boolean createShort = false;
	// END PROPERTIES
	
	@Override
	public Chnl create() throws CreateException {
		
		Chnl chnlOut = ChnlFactory.instance().createEmptyInitialised(
			dimProvider.create(),
			VoxelDataTypeByte.instance
		);
		VoxelBox<ByteBuffer> vbOut = chnlOut.getVoxelBox().asByte();
		
		ObjMaskCollection objs = objMaskProvider.create();
		
		for( ObjMask om : objs ) {
			BinaryVoxelBox<ByteBuffer> bvb = om.binaryVoxelBox().duplicate();
			VoxelBox<ByteBuffer> vbDist = ChnlProviderDistanceTransformExact3D.createDistanceMapForVoxelBox(
				bvb,
				chnlOut.getDimensions().getRes(),
				suppressZ,
				1.0,
				1.0,
				createShort,
				false
			);
			
			BoundingBox bboxSrc = new BoundingBox(vbDist.extnt());
			vbDist.copyPixelsToCheckMask(bboxSrc, vbOut, om.getBoundingBox(), om.getVoxelBox(), om.getBinaryValuesByte());
		}
		
		return chnlOut;
	}

	public boolean isSuppressZ() {
		return suppressZ;
	}

	public void setSuppressZ(boolean suppressZ) {
		this.suppressZ = suppressZ;
	}

	public ObjMaskProvider getObjMaskProvider() {
		return objMaskProvider;
	}

	public void setObjMaskProvider(ObjMaskProvider objMaskProvider) {
		this.objMaskProvider = objMaskProvider;
	}

	public boolean isCreateShort() {
		return createShort;
	}

	public void setCreateShort(boolean createShort) {
		this.createShort = createShort;
	}

	public ImageDimProvider getDimProvider() {
		return dimProvider;
	}

	public void setDimProvider(ImageDimProvider dimProvider) {
		this.dimProvider = dimProvider;
	}

}
