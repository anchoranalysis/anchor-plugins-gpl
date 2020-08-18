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
package org.anchoranalysis.plugin.fiji.bean.mask.provider.channel;

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.binary.mask.Mask;
import org.anchoranalysis.image.binary.mask.MaskFactory;
import org.anchoranalysis.image.binary.values.BinaryValues;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.stack.Stack;
import org.anchoranalysis.image.voxel.buffer.VoxelBufferByte;
import org.anchoranalysis.plugin.image.bean.mask.provider.FromChannelBase;

/**
 * Applies local thresholding algorithm using Fiji's {link Auto_Local_Threshold} plugin
 * 
 * <p>The thresholding procedure it applied to each slice independently.
 * 
 * @author Owen Feehan
 *
 */
public class AutoLocalThreshold extends FromChannelBase {

    // START BEAN PROPERTIES
    // "Bernsen", "Mean", "Median", "MidGrey", "Niblack", "Sauvola"
    @BeanField @Getter @Setter private String method = "";

    @BeanField @Getter @Setter private int radius = 15;
    // END BEAN PROPERTIES

    @Override
    protected Mask createFromSource(Channel chnl) throws CreateException {

        // TODO is this duplication needed?
        chnl = chnl.duplicate();

        Stack stack = new Stack(chnl);

        // The default binary values for OFF (0) and ON (255) match the output from the plugin
        Mask out = MaskFactory.createMaskOff(chnl.dimensions(), BinaryValues.getDefault());

        Auto_Local_Threshold at = new Auto_Local_Threshold();

        chnl.extent().iterateOverZ( z-> {
            VoxelBufferByte thresholded = thresholdSlice(stack.extractSlice(z), at);
            out.voxels().replaceSlice(z, thresholded );
        });

        return out;
    }
    
    private VoxelBufferByte thresholdSlice(Stack slice, Auto_Local_Threshold at) {
        ImagePlus imagePlus = IJWrap.createImagePlus(slice, false);

        Object[] ret = at.exec(imagePlus, method, radius, 0, 0, true);
        ImagePlus ipOut = (ImagePlus) ret[0];

        ImageProcessor processor = ipOut.getImageStack().getProcessor(1);
        return VoxelBufferByte.wrap((byte[]) processor.getPixels());
    }
}
