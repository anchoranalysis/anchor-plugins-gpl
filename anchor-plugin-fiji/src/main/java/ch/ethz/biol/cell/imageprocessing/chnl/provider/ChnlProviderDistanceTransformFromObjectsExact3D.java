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
package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.binary.voxel.BinaryVoxels;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactory;
import org.anchoranalysis.image.extent.BoundingBox;
import org.anchoranalysis.image.extent.ImageDimensions;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.voxel.Voxels;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;

// Euclidian distance transform from ImageJ
//
// Does not re-use the binary image
public class ChnlProviderDistanceTransformFromObjectsExact3D extends ChnlProviderDimSource {

    // START PROPERTIES
    @BeanField @Getter @Setter private ObjectCollectionProvider objects;

    @BeanField @Getter @Setter private boolean suppressZ = false;

    @BeanField @Getter @Setter private boolean createShort = false;
    // END PROPERTIES

    @Override
    protected Channel createFromDim(ImageDimensions dimensions) throws CreateException {

        Channel chnlOut =
                ChannelFactory.instance()
                        .createEmptyInitialised(dimensions, VoxelDataTypeUnsignedByte.INSTANCE);
        Voxels<ByteBuffer> voxelsOut = chnlOut.voxels().asByte();

        for (ObjectMask object : objects.create()) {
            BinaryVoxels<ByteBuffer> voxels = object.binaryVoxels().duplicate();
            Voxels<ByteBuffer> voxelsDistance =
                    ChnlProviderDistanceTransformExact3D.createDistanceMapForVoxels(
                            voxels,
                            chnlOut.dimensions().resolution(),
                            suppressZ,
                            1.0,
                            1.0,
                            createShort,
                            false);

            BoundingBox boxSrc = new BoundingBox(voxelsDistance.extent());
            voxelsDistance.copyPixelsToCheckMask(
                    boxSrc,
                    voxelsOut,
                    object.boundingBox(),
                    object.voxels(),
                    object.binaryValuesByte());
        }

        return chnlOut;
    }
}
