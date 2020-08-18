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
import org.anchoranalysis.image.binary.voxel.BinaryVoxels;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactory;
import org.anchoranalysis.image.extent.ImageDimensions;
import org.anchoranalysis.image.extent.ImageResolution;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverter;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverterToUnsignedByte;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverterToUnsignedShort;
import org.anchoranalysis.image.stack.region.chnlconverter.ConversionPolicy;
import org.anchoranalysis.image.voxel.Voxels;
import org.anchoranalysis.image.voxel.datatype.VoxelDataType;
import org.anchoranalysis.image.voxel.datatype.Float;
import org.anchoranalysis.image.voxel.datatype.UnsignedByte;
import org.anchoranalysis.image.voxel.datatype.UnsignedShort;

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
    public static Voxels<ByteBuffer> createDistanceMapForVoxels(
            BinaryVoxels<ByteBuffer> bvb,
            ImageResolution res,
            boolean suppressZ,
            double multiplyBy,
            double multiplyByZRes,
            boolean createShort,
            boolean applyRes)
            throws CreateException {
        Channel chnlIn =
                ChannelFactory.instance()
                        .get(UnsignedByte.INSTANCE)
                        .create(bvb.voxels(), res);
        Mask mask = new Mask(chnlIn, bvb.binaryValues());

        Channel distanceMap =
                createDistanceMapForMask(
                        mask, suppressZ, multiplyBy, multiplyByZRes, createShort, applyRes);
        return distanceMap.voxels().asByte();
    }

    public static Channel createDistanceMapForMask(
            Mask mask,
            boolean suppressZ,
            double multiplyBy,
            double multiplyByZRes,
            boolean createShort,
            boolean applyRes)
            throws CreateException {
        if (mask.binaryValues().getOnInt() != 255) {
            throw new CreateException("Binary On must be 255");
        }

        if (mask.binaryValues().getOffInt() != 0) {
            throw new CreateException("Binary Off must be 0");
        }

        if (mask.dimensions().extent().z() > 1 && !suppressZ) {

            double zRelRes = mask.dimensions().resolution().getZRelativeResolution();
            if (Double.isNaN(zRelRes)) {
                throw new CreateException("Z-resolution is NaN");
            }

            if (zRelRes == 0) {
                throw new CreateException("Z-resolution is 0");
            }
        }

        if (suppressZ) {

            Channel chnlOut = createEmptyChnl(createShort, mask.dimensions());

            for (int z = 0; z < mask.dimensions().extent().z(); z++) {
                Mask slice = mask.extractSlice(z);
                Channel distanceSlice =
                        createDistanceMapForChnlFromPlugin(
                                slice, true, multiplyBy, multiplyByZRes, createShort, applyRes);
                chnlOut.voxels().transferSlice(z, distanceSlice.voxels(), 0, true);
            }

            return chnlOut;

        } else {
            return createDistanceMapForChnlFromPlugin(
                    mask, false, multiplyBy, multiplyByZRes, createShort, applyRes);
        }
    }

    private static Channel createEmptyChnl(boolean createShort, ImageDimensions dims) {
        VoxelDataType dataType =
                createShort
                        ? UnsignedShort.INSTANCE
                        : UnsignedByte.INSTANCE;
        return ChannelFactory.instance().createUninitialised(dims, dataType);
    }

    @Override
    protected Channel createFromMask(Mask mask) throws CreateException {
        return createDistanceMapForMask(
                mask, suppressZ, multiplyBy, multiplyByZRes, createShort, applyRes);
    }

    private static Channel createDistanceMapForChnlFromPlugin(
            Mask mask,
            boolean suppressZ,
            double multFactor,
            double multFactorZ,
            boolean createShort,
            boolean applyRes) {

        // Assumes X and Y have the same resolution

        Channel distanceAsFloat =
                EDT.compute(
                        mask,
                        ChannelFactory.instance().get(Float.INSTANCE),
                        suppressZ,
                        multFactorZ);

        double factor = multiplactionFactor(multFactor, applyRes, mask);
        distanceAsFloat.arithmetic().multiplyBy(factor);

        ChannelConverter<?> converter =
                createShort
                        ? new ChannelConverterToUnsignedShort()
                        : new ChannelConverterToUnsignedByte();
        return converter.convert(distanceAsFloat, ConversionPolicy.CHANGE_EXISTING_CHANNEL);
    }

    private static double multiplactionFactor(double multFactor, boolean applyRes, Mask mask) {
        if (applyRes) {
            return multFactor * mask.dimensions().resolution().x();
        } else {
            return multFactor;
        }
    }
}
