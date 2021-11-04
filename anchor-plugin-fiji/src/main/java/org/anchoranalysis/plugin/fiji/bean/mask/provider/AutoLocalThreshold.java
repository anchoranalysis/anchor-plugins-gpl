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
package org.anchoranalysis.plugin.fiji.bean.mask.provider;

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.exception.CreateException;
import org.anchoranalysis.image.core.channel.Channel;
import org.anchoranalysis.image.core.mask.Mask;
import org.anchoranalysis.image.core.mask.MaskFactory;
import org.anchoranalysis.image.core.stack.Stack;
import org.anchoranalysis.image.voxel.binary.values.BinaryValuesInt;
import org.anchoranalysis.image.voxel.buffer.VoxelBuffer;
import org.anchoranalysis.image.voxel.buffer.primitive.UnsignedByteBuffer;
import org.anchoranalysis.io.imagej.convert.ConvertToImagePlus;
import org.anchoranalysis.io.imagej.convert.ConvertToVoxelBuffer;
import org.anchoranalysis.io.imagej.convert.ImageJConversionException;
import org.anchoranalysis.plugin.image.bean.mask.provider.FromChannelBase;

/**
 * Applies local thresholding algorithm using Fiji's {link Auto_Local_Threshold} plugin
 *
 * <p>The thresholding procedure it applied to each slice independently.
 *
 * @author Owen Feehan
 */
public class AutoLocalThreshold extends FromChannelBase {

    // START BEAN PROPERTIES
    // "Bernsen", "Mean", "Median", "MidGrey", "Niblack", "Sauvola"
    @BeanField @Getter @Setter private String method = "";

    @BeanField @Getter @Setter private int radius = 15;
    // END BEAN PROPERTIES

    @Override
    protected Mask createFromSource(Channel channel) throws CreateException {

        // TODO is this duplication needed?
        channel = channel.duplicate();

        Stack stack = new Stack(channel);

        // The default binary values for <i>off</i> (0) and <i>on</i> (255) match the output from the plugin
        Mask out = MaskFactory.createMaskOff(channel.dimensions(), BinaryValuesInt.getDefault());

        Auto_Local_Threshold at = new Auto_Local_Threshold();

        channel.extent()
                .iterateOverZ(
                        z -> {
                            VoxelBuffer<UnsignedByteBuffer> thresholded =
                                    thresholdSlice(stack.extractSlice(z), at);
                            out.voxels().replaceSlice(z, thresholded);
                        });

        return out;
    }

    private VoxelBuffer<UnsignedByteBuffer> thresholdSlice(Stack slice, Auto_Local_Threshold at)
            throws CreateException {
        try {
            ImagePlus imagePlus = ConvertToImagePlus.from(slice, false);

            Object[] ret = at.exec(imagePlus, method, radius, 0, 0, true);
            ImagePlus imageOut = (ImagePlus) ret[0];

            ImageProcessor processor = imageOut.getImageStack().getProcessor(1);
            return ConvertToVoxelBuffer.asByte(processor);
        } catch (ImageJConversionException e) {
            throw new CreateException(e);
        }
    }
}
