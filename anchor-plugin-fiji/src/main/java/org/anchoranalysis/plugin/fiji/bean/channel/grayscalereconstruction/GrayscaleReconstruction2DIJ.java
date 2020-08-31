/*-
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2010 - 2020 Owen Feehan, ETH Zurich, University of Zurich, Hoffmann-La Roche
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
package org.anchoranalysis.plugin.fiji.bean.channel.grayscalereconstruction;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.voxel.Voxels;
import org.anchoranalysis.image.voxel.VoxelsWrapper;
import org.anchoranalysis.image.voxel.datatype.UnsignedByteVoxelType;
import org.anchoranalysis.io.imagej.convert.ConvertFromImagePlus;
import org.anchoranalysis.io.imagej.convert.ConvertToImageProcessor;
import org.anchoranalysis.plugin.image.bean.object.segment.channel.watershed.minima.grayscalereconstruction.GrayscaleReconstructionByErosion;

public class GrayscaleReconstruction2DIJ extends GrayscaleReconstructionByErosion {

    @Override
    public VoxelsWrapper reconstruction(
            VoxelsWrapper mask, VoxelsWrapper marker, Optional<ObjectMask> containingMask)
            throws OperationFailedException {

        if (containingMask.isPresent()) {
            throw new OperationFailedException("A mask is not supported for this operation");
        }

        if (!marker.getVoxelDataType().equals(UnsignedByteVoxelType.INSTANCE)
                || !mask.getVoxelDataType().equals(UnsignedByteVoxelType.INSTANCE)) {
            throw new OperationFailedException("Only unsigned byte supported for marker image");
        }

        Voxels<ByteBuffer> markerCast = marker.asByte();
        Voxels<ByteBuffer> maskCast = mask.asByte();

        // We flip everything because the IJ plugin is reconstruction by Dilation, whereas we want
        // reconstruction by Erosion
        markerCast.arithmetic().subtractFrom(UnsignedByteVoxelType.MAX_VALUE_INT);
        mask.subtractFromMaxValue();

        Voxels<ByteBuffer> ret = reconstructionByDilation(maskCast, markerCast);
        ret.arithmetic().subtractFrom(UnsignedByteVoxelType.MAX_VALUE_INT);

        return new VoxelsWrapper(ret);
    }

    private Voxels<ByteBuffer> reconstructionByDilation(
            Voxels<ByteBuffer> mask, Voxels<ByteBuffer> marker) {

        ImageProcessor processorMask = ConvertToImageProcessor.fromByte(mask.slices(), 0);
        ImageProcessor processorMarker = ConvertToImageProcessor.fromByte(marker.slices(), 0);

        ImagePlus imageMask = new ImagePlus("mask", processorMask);
        ImagePlus imageMarker = new ImagePlus("marker", processorMarker);

        GreyscaleReconstruct_ gr = new GreyscaleReconstruct_();
        Object[] ret = gr.exec(imageMask, imageMarker, "recon", true, false);

        ImagePlus ipRecon = (ImagePlus) ret[1];
        return ConvertFromImagePlus.toVoxels(ipRecon).asByte();
    }
}
