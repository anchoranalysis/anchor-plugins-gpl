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
import java.util.Optional;
import org.anchoranalysis.core.exception.OperationFailedException;
import org.anchoranalysis.image.voxel.Voxels;
import org.anchoranalysis.image.voxel.VoxelsUntyped;
import org.anchoranalysis.image.voxel.buffer.primitive.UnsignedByteBuffer;
import org.anchoranalysis.image.voxel.datatype.UnsignedByteVoxelType;
import org.anchoranalysis.image.voxel.object.ObjectMask;
import org.anchoranalysis.io.imagej.convert.ConvertFromImagePlus;
import org.anchoranalysis.io.imagej.convert.ConvertToImagePlus;
import org.anchoranalysis.plugin.image.bean.object.segment.channel.watershed.minima.grayscalereconstruction.GrayscaleReconstructionByErosion;

/** Performs 2D grayscale reconstruction using ImageJ's implementation. */
public class GrayscaleReconstruction2DIJ extends GrayscaleReconstructionByErosion {

    @Override
    public VoxelsUntyped reconstruction(
            VoxelsUntyped mask, VoxelsUntyped marker, Optional<ObjectMask> containingMask)
            throws OperationFailedException {

        if (containingMask.isPresent()) {
            throw new OperationFailedException("A mask is not supported for this operation");
        }

        if (!marker.getVoxelDataType().equals(UnsignedByteVoxelType.INSTANCE)
                || !mask.getVoxelDataType().equals(UnsignedByteVoxelType.INSTANCE)) {
            throw new OperationFailedException("Only unsigned byte supported for marker image");
        }

        Voxels<UnsignedByteBuffer> markerCast = marker.asByte();
        Voxels<UnsignedByteBuffer> maskCast = mask.asByte();

        // We flip everything because the IJ plugin is reconstruction by Dilation, whereas we want
        // reconstruction by Erosion
        markerCast.arithmetic().subtractFrom(UnsignedByteVoxelType.MAX_VALUE_INT);
        mask.subtractFromMaxValue();

        Voxels<UnsignedByteBuffer> ret = reconstructionByDilation(maskCast, markerCast);
        ret.arithmetic().subtractFrom(UnsignedByteVoxelType.MAX_VALUE_INT);

        return new VoxelsUntyped(ret);
    }

    /**
     * Performs reconstruction by dilation using ImageJ's GreyscaleReconstruct_ plugin.
     *
     * @param mask the mask {@link Voxels}
     * @param marker the marker {@link Voxels}
     * @return the reconstructed {@link Voxels}
     */
    private Voxels<UnsignedByteBuffer> reconstructionByDilation(
            Voxels<UnsignedByteBuffer> mask, Voxels<UnsignedByteBuffer> marker) {

        ImagePlus imageMask = ConvertToImagePlus.fromSlice(mask, 0, "mask");
        ImagePlus imageMarker = ConvertToImagePlus.fromSlice(marker, 0, "marker");

        GreyscaleReconstruct_ gr = new GreyscaleReconstruct_();
        Object[] ret = gr.exec(imageMask, imageMarker, "recon", true, false);

        ImagePlus ipRecon = (ImagePlus) ret[1];
        return ConvertFromImagePlus.toVoxels(ipRecon).asByte();
    }
}
