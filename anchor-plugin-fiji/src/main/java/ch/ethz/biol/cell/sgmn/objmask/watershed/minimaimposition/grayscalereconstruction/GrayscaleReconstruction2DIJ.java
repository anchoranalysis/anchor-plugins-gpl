package ch.ethz.biol.cell.sgmn.objmask.watershed.minimaimposition.grayscalereconstruction;

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


import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.box.VoxelBoxWrapper;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;
import org.anchoranalysis.plugin.image.bean.sgmn.watershed.minima.grayscalereconstruction.GrayscaleReconstructionByErosion;

import ch.ethz.biol.cell.nucleii.landini.GreyscaleReconstruct_;

public class GrayscaleReconstruction2DIJ extends GrayscaleReconstructionByErosion {
	
	@Override
	public VoxelBoxWrapper reconstruction( VoxelBoxWrapper mask, VoxelBoxWrapper marker, Optional<ObjectMask> containingMask) throws OperationFailedException {
		
		if (containingMask.isPresent()) {
			throw new OperationFailedException("A mask is not supported for this operation");
		}
		
		if (!marker.getVoxelDataType().equals(VoxelDataTypeUnsignedByte.instance) || !mask.getVoxelDataType().equals(VoxelDataTypeUnsignedByte.instance)) {
			throw new OperationFailedException("Only unsigned byte supported for marker image");
		}
		
		VoxelBox<ByteBuffer> markerCast = marker.asByte();
		VoxelBox<ByteBuffer> maskCast = mask.asByte();
		
		// We flip everything because the IJ plugin is reconstruction by Dilation, whereas we want reconstruction by Erosion
		markerCast.subtractFrom( VoxelDataTypeUnsignedByte.MAX_VALUE_INT );
		mask.subtractFromMaxValue();
		
		VoxelBox<ByteBuffer> ret = reconstructionByDilation(maskCast, markerCast);
		ret.subtractFrom( VoxelDataTypeUnsignedByte.MAX_VALUE_INT );
		
		return new VoxelBoxWrapper(ret);
	}

	private VoxelBox<ByteBuffer> reconstructionByDilation( VoxelBox<ByteBuffer> maskVb, VoxelBox<ByteBuffer> markerVb ) {
		
		ImageProcessor processorMask = IJWrap.imageProcessorByte(maskVb.getPlaneAccess(), 0);
		ImageProcessor processorMarker = IJWrap.imageProcessorByte(markerVb.getPlaneAccess(), 0);
		
		ImagePlus ipMaskImage = new ImagePlus("mask", processorMask );
		ImagePlus ipMarkerImage = new ImagePlus("marker", processorMarker );
		
		GreyscaleReconstruct_ gr = new GreyscaleReconstruct_();
		Object[] ret = gr.exec( ipMaskImage, ipMarkerImage, "recon", true, false);
		
		ImagePlus ipRecon = (ImagePlus) ret[1];
		return IJWrap.voxelBoxFromImagePlus(ipRecon).asByte();
	}
}
