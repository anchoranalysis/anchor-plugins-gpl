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
import org.anchoranalysis.image.binary.mask.Mask;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBox;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactory;
import org.anchoranalysis.image.extent.ImageDimensions;
import org.anchoranalysis.image.extent.ImageResolution;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverter;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverterToUnsignedByte;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverterToUnsignedShort;
import org.anchoranalysis.image.stack.region.chnlconverter.ConversionPolicy;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.datatype.VoxelDataType;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeFloat;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedShort;

/**
 * Euclidian Distance Transform from ImageJ that works on 2D as well as 3D z-stacks.
 *
 * <p>See <a href="https://en.wikipedia.org/wiki/Distance_transform">Distance transform on
 * Wikipedia</a>.
 *
 * <p>A new channel is always created i.e. the input channel is unchanged.
 *
 * <p>The plugin uses aspect ratio (relative distance between z and xy slices) in its distance
 * calculations.
 *
 * @author Owen Feehan
 */
public class ChnlProviderDistanceTransformExact3D extends ChnlProviderMask {

    // START PROPERTIES
    @BeanField @Getter @Setter private boolean suppressZ = false;

    @BeanField @Getter @Setter private double multiplyBy = 1.0;

    @BeanField @Getter @Setter private double multiplyByZRes = 1.0;

    @BeanField @Getter @Setter private boolean createShort = false;

    @BeanField @Getter @Setter private boolean applyRes = false;
    // END PROPERTIES

    // We can also change a binary voxel buffer
    public static VoxelBox<ByteBuffer> createDistanceMapForVoxelBox(
            BinaryVoxelBox<ByteBuffer> bvb,
            ImageResolution res,
            boolean suppressZ,
            double multiplyBy,
            double multiplyByZRes,
            boolean createShort,
            boolean applyRes)
            throws CreateException {
        Channel chnlIn =
                ChannelFactory.instance()
                        .get(VoxelDataTypeUnsignedByte.INSTANCE)
                        .create(bvb.getVoxels(), res);
        Mask mask = new Mask(chnlIn, bvb.getBinaryValues());

        Channel distanceMap =
                createDistanceMapForChnl(
                        mask, suppressZ, multiplyBy, multiplyByZRes, createShort, applyRes);
        return distanceMap.voxels().asByte();
    }

    public static Channel createDistanceMapForChnl(
            Mask chnl,
            boolean suppressZ,
            double multiplyBy,
            double multiplyByZRes,
            boolean createShort,
            boolean applyRes)
            throws CreateException {
        if (chnl.getBinaryValues().getOnInt() != 255) {
            throw new CreateException("Binary On must be 255");
        }

        if (chnl.getBinaryValues().getOffInt() != 0) {
            throw new CreateException("Binary Off must be 0");
        }

        if (chnl.getDimensions().getExtent().getZ() > 1 && !suppressZ) {

            double zRelRes = chnl.getDimensions().getResolution().getZRelativeResolution();
            if (Double.isNaN(zRelRes)) {
                throw new CreateException("Z-resolution is NaN");
            }

            if (zRelRes == 0) {
                throw new CreateException("Z-resolution is 0");
            }
        }

        if (suppressZ) {

            Channel chnlOut = createEmptyChnl(createShort, chnl.getDimensions());

            for (int z = 0; z < chnl.getDimensions().getExtent().getZ(); z++) {
                Mask chnlSlice = chnl.extractSlice(z);
                Channel distanceSlice =
                        createDistanceMapForChnlFromPlugin(
                                chnlSlice, true, multiplyBy, multiplyByZRes, createShort, applyRes);
                chnlOut.voxels()
                        .transferPixelsForPlane(z, distanceSlice.voxels(), 0, true);
            }

            return chnlOut;

        } else {
            return createDistanceMapForChnlFromPlugin(
                    chnl, false, multiplyBy, multiplyByZRes, createShort, applyRes);
        }
    }

    private static Channel createEmptyChnl(boolean createShort, ImageDimensions dims) {
        VoxelDataType dataType =
                createShort
                        ? VoxelDataTypeUnsignedShort.INSTANCE
                        : VoxelDataTypeUnsignedByte.INSTANCE;
        return ChannelFactory.instance().createEmptyUninitialised(dims, dataType);
    }

    @Override
    protected Channel createFromMask(Mask mask) throws CreateException {
        return createDistanceMapForChnl(
                mask, suppressZ, multiplyBy, multiplyByZRes, createShort, applyRes);
    }

    private static Channel createDistanceMapForChnlFromPlugin(
            Mask chnl,
            boolean suppressZ,
            double multFactor,
            double multFactorZ,
            boolean createShort,
            boolean applyRes) {

        // Assumes X and Y have the same resolution

        Channel distanceAsFloat =
                EDT.compute(
                        chnl,
                        ChannelFactory.instance().get(VoxelDataTypeFloat.INSTANCE),
                        suppressZ,
                        multFactorZ);

        if (applyRes) {
            distanceAsFloat
                    .voxels()
                    .any()
                    .multiplyBy(multFactor * chnl.getDimensions().getResolution().getX());
        } else {
            distanceAsFloat.voxels().any().multiplyBy(multFactor);
        }

        ChannelConverter<?> converter =
                createShort
                        ? new ChannelConverterToUnsignedShort()
                        : new ChannelConverterToUnsignedByte();
        return converter.convert(distanceAsFloat, ConversionPolicy.CHANGE_EXISTING_CHANNEL);
    }
}
