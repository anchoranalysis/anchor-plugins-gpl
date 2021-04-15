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
package org.anchoranalysis.plugin.fiji.bean.channel.provider.distance;

import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.exception.CreateException;
import org.anchoranalysis.image.core.channel.Channel;
import org.anchoranalysis.image.core.channel.convert.ChannelConverter;
import org.anchoranalysis.image.core.channel.convert.ConversionPolicy;
import org.anchoranalysis.image.core.channel.convert.ToUnsignedByte;
import org.anchoranalysis.image.core.channel.convert.ToUnsignedShort;
import org.anchoranalysis.image.core.channel.factory.ChannelFactory;
import org.anchoranalysis.image.core.dimensions.Dimensions;
import org.anchoranalysis.image.core.dimensions.Resolution;
import org.anchoranalysis.image.core.mask.Mask;
import org.anchoranalysis.image.voxel.Voxels;
import org.anchoranalysis.image.voxel.binary.BinaryVoxels;
import org.anchoranalysis.image.voxel.buffer.primitive.UnsignedByteBuffer;
import org.anchoranalysis.image.voxel.datatype.FloatVoxelType;
import org.anchoranalysis.image.voxel.datatype.UnsignedByteVoxelType;
import org.anchoranalysis.image.voxel.datatype.UnsignedShortVoxelType;
import org.anchoranalysis.image.voxel.datatype.VoxelDataType;
import org.anchoranalysis.plugin.image.bean.channel.provider.mask.FromMaskBase;

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
public class DistanceTransform3D extends FromMaskBase {

    // START PROPERTIES
    @BeanField @Getter @Setter private boolean suppressZ = false;

    @BeanField @Getter @Setter private double multiplyBy = 1.0;

    @BeanField @Getter @Setter private boolean createShort = false;

    /** Multiples the values by the x-resolution, if it exists. */
    @BeanField @Getter @Setter private boolean applyResolution = false;
    // END PROPERTIES

    // We can also change a binary voxel buffer
    public static Voxels<UnsignedByteBuffer> createDistanceMapForVoxels(
            BinaryVoxels<UnsignedByteBuffer> voxels,
            Optional<Resolution> resolution,
            boolean suppressZ,
            double multiplyBy,
            double multiplyByZRes,
            boolean createShort,
            boolean applyRes)
            throws CreateException {
        Channel channel =
                ChannelFactory.instance()
                        .get(UnsignedByteVoxelType.INSTANCE)
                        .create(voxels.voxels(), resolution);
        Mask mask = new Mask(channel, voxels.binaryValues());

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

        // Performs some checks on the z-resolution, if it exits
        if (mask.resolution().isPresent() && mask.extent().z() > 1 && !suppressZ) {
            checkZResolution(mask.resolution().get()); // NOSONAR
        }

        if (suppressZ) {

            Channel channelOut = createEmptyChannel(createShort, mask.dimensions());

            for (int z = 0; z < mask.dimensions().extent().z(); z++) {
                Mask slice = mask.extractSlice(z);
                Channel distanceSlice =
                        createDistanceMapForChannelFromPlugin(
                                slice, true, multiplyBy, multiplyByZRes, createShort, applyRes);
                channelOut.voxels().transferSlice(z, distanceSlice.voxels(), 0, true);
            }

            return channelOut;

        } else {
            return createDistanceMapForChannelFromPlugin(
                    mask, false, multiplyBy, multiplyByZRes, createShort, applyRes);
        }
    }

    private static Channel createEmptyChannel(boolean createShort, Dimensions dims) {
        VoxelDataType dataType =
                createShort ? UnsignedShortVoxelType.INSTANCE : UnsignedByteVoxelType.INSTANCE;
        return ChannelFactory.instance().createUninitialised(dims, dataType);
    }

    @Override
    protected Channel createFromMask(Mask mask) throws CreateException {
        return createDistanceMapForMask(
                mask, suppressZ, multiplyBy, 1, createShort, applyResolution);
    }

    private static Channel createDistanceMapForChannelFromPlugin(
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
                        ChannelFactory.instance().get(FloatVoxelType.INSTANCE),
                        suppressZ,
                        multFactorZ);

        double factor = multiplicationFactor(multFactor, applyRes, mask);
        distanceAsFloat.arithmetic().multiplyBy(factor);

        ChannelConverter<?> converter = createShort ? new ToUnsignedShort() : new ToUnsignedByte();
        return converter.convert(distanceAsFloat, ConversionPolicy.CHANGE_EXISTING_CHANNEL);
    }

    private static double multiplicationFactor(
            double multFactor, boolean applyResolution, Mask mask) {
        if (applyResolution && mask.resolution().isPresent()) {
            return multFactor * mask.resolution().get().x(); // NOSONAR
        } else {
            return multFactor;
        }
    }

    private static void checkZResolution(Resolution resolution) throws CreateException {
        double zRelRes = resolution.zRelative();
        if (Double.isNaN(zRelRes)) {
            throw new CreateException("Z-resolution is NaN");
        }

        if (zRelRes == 0) {
            throw new CreateException("Z-resolution is 0");
        }
    }
}
