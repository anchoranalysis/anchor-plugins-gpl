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
package ch.ethz.biol.cell.imageprocessing.binaryimgchnl.provider;

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.binary.mask.Mask;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactory;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.stack.Stack;
import org.anchoranalysis.image.voxel.Voxels;
import org.anchoranalysis.image.voxel.buffer.VoxelBufferByte;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;

public class BinaryChnlProviderAutoLocalThrshld extends BinaryChnlProviderChnlSource {

    // START BEAN PROPERTIES
    // "Bernsen", "Mean", "Median", "MidGrey", "Niblack", "Sauvola"
    @BeanField @Getter @Setter private String method = "";

    @BeanField @Getter @Setter private int radius = 15;
    // END BEAN PROPERTIES

    @Override
    protected Mask createFromSource(Channel chnl) throws CreateException {

        chnl = chnl.duplicate();

        Stack stack = new Stack(chnl);

        Channel chnlOut =
                ChannelFactory.instance()
                        .createEmptyUninitialised(
                                chnl.getDimensions(), VoxelDataTypeUnsignedByte.INSTANCE);

        Voxels<ByteBuffer> vb = chnlOut.voxels().asByte();

        Auto_Local_Threshold at = new Auto_Local_Threshold();

        for (int z = 0; z < chnl.getDimensions().getZ(); z++) {
            ImagePlus ip = IJWrap.createImagePlus(stack.extractSlice(z), false);

            Object[] ret = at.exec(ip, method, radius, 0, 0, true);
            ImagePlus ipOut = (ImagePlus) ret[0];

            ImageProcessor processor = ipOut.getImageStack().getProcessor(1);
            byte[] arr = (byte[]) processor.getPixels();
            vb.setPixelsForPlane(z, VoxelBufferByte.wrap(arr));
        }

        return new Mask(chnlOut);
    }
}
