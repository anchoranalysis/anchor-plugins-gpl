package ch.ethz.biol.cell.sgmn.objmask.watershed.minimaimposition.grayscalereconstruction;

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


import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.nio.ByteBuffer;

import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.objmask.ObjMask;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.box.VoxelBoxWrapper;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeByte;

import ch.ethz.biol.cell.nucleii.landini.GreyscaleReconstruct_;

public class GrayscaleReconstruction2DIJ extends GrayscaleReconstructionByErosion {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// START BEAN PROPERTIES
	
	// END BEAN PROPERTIES

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
	
	@Override
	public VoxelBoxWrapper reconstruction( VoxelBoxWrapper maskVb, VoxelBoxWrapper markerVb ) throws OperationFailedException {
		
		if (!markerVb.getVoxelDataType().equals(VoxelDataTypeByte.instance) || !maskVb.getVoxelDataType().equals(VoxelDataTypeByte.instance)) {
			throw new OperationFailedException("Only unsigned byte supported for marker image");
		}
		
		VoxelBox<ByteBuffer> markerCast = markerVb.asByte();
		VoxelBox<ByteBuffer> maskCast = maskVb.asByte();
		
		// We flip everything because the IJ plugin is reconstruction by Dilation, whereas we want reconstruction by Erosion
		markerCast.subtractFrom( VoxelDataTypeByte.MAX_VALUE_INT );
		maskVb.subtractFromMaxValue();
		
		VoxelBox<ByteBuffer> ret = reconstructionByDilation(maskCast, markerCast);
		ret.subtractFrom( VoxelDataTypeByte.MAX_VALUE_INT );
		
		return new VoxelBoxWrapper(ret);
	}

	@Override
	public VoxelBoxWrapper reconstruction(
			VoxelBoxWrapper maskImg, VoxelBoxWrapper markerImg,
			ObjMask containingMask)
			throws OperationFailedException {
		throw new OperationFailedException("Does not support a containing mask");
	}
}
