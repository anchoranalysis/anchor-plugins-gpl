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


import java.nio.ByteBuffer;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ObjMaskProvider;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBox;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.chnl.factory.ChnlFactory;
import org.anchoranalysis.image.extent.BoundingBox;
import org.anchoranalysis.image.extent.ImageDim;
import org.anchoranalysis.image.objmask.ObjMask;
import org.anchoranalysis.image.objmask.ObjMaskCollection;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;


// Euclidian distance transform from ImageJ
//
// Does not re-use the binary image
public class ChnlProviderDistanceTransformFromObjsExact3D extends ChnlProviderDimSource {

	// START PROPERTIES
	@BeanField
	private ObjMaskProvider objs;
	
	@BeanField
	private boolean suppressZ = false;
	
	@BeanField
	private boolean createShort = false;
	// END PROPERTIES
	
	@Override
	protected Chnl createFromDim(ImageDim dim) throws CreateException {

		Chnl chnlOut = ChnlFactory.instance().createEmptyInitialised(
			dim,
			VoxelDataTypeUnsignedByte.instance
		);
		VoxelBox<ByteBuffer> vbOut = chnlOut.getVoxelBox().asByte();
		
		ObjMaskCollection objsCollection = objs.create();
		
		for( ObjMask om : objsCollection ) {
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

	public ObjMaskProvider getObjs() {
		return objs;
	}

	public void setObjs(ObjMaskProvider objs) {
		this.objs = objs;
	}

	public boolean isCreateShort() {
		return createShort;
	}

	public void setCreateShort(boolean createShort) {
		this.createShort = createShort;
	}
}
