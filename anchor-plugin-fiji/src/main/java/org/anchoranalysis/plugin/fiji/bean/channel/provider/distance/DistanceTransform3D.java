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
import java.util.function.ToDoubleFunction;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.xml.exception.ProvisionFailedException;
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
 * <p>As a simplification, when resolution is used, the XY plane is multipled by the average
 * of the x and y dimensions.
 *
 * @author Owen Feehan
 */
public class DistanceTransform3D extends FromMaskBase {

    // START PROPERTIES
    @BeanField @Getter @Setter private boolean suppressZ = false;

    @BeanField @Getter @Setter private float multiplyBy = 1.0f;

    @BeanField @Getter @Setter private boolean createShort = false;

    /** Multiples the values by the x-resolution, if it exists. */
    @BeanField @Getter @Setter private boolean applyResolution = false;
    
    /** If the z-resolution is undefined, the z dimenion is ignored. */
    @BeanField @Getter @Setter private boolean ignoreZIfNaN = true;
    // END PROPERTIES

    @Override
    protected Channel createFromMask(Mask mask) throws ProvisionFailedException {
        return createDistanceMapForMask(
                mask, 1);
    }
    
    // We can also change a binary voxel buffer
    public Voxels<UnsignedByteBuffer> createDistanceMapForVoxels(
            BinaryVoxels<UnsignedByteBuffer> voxels,
            Optional<Resolution> resolution,
            float multiplyByZRes)
            throws ProvisionFailedException {
        Channel channel =
                ChannelFactory.instance()
                        .get(UnsignedByteVoxelType.INSTANCE)
                        .create(voxels.voxels(), resolution);
        Mask mask = new Mask(channel, voxels.binaryValues());

        Channel distanceMap =
                createDistanceMapForMask(
                        mask, multiplyByZRes);
        return distanceMap.voxels().asByte();
    }

    private Channel createDistanceMapForMask(
            Mask mask,
            float multiplyByZRes)
            throws ProvisionFailedException {
        if (mask.binaryValues().getOnInt() != 255) {
            throw new ProvisionFailedException("Binary On must be 255");
        }

        if (mask.binaryValues().getOffInt() != 0) {
            throw new ProvisionFailedException("Binary Off must be 0");
        }

        // Performs some checks on the z-resolution, if it exists
        if (mask.resolution().isPresent() && mask.extent().z() > 1 && !suppressZ) {
            checkZResolution(mask.resolution().get()); // NOSONAR
        }
        
        boolean excludeZDimension = suppressZ || hasNanZResolution(mask.resolution()); 

        if (excludeZDimension) {

            Channel channelOut = createEmptyChannel(createShort, mask.dimensions());

            for (int z = 0; z < mask.dimensions().extent().z(); z++) {
                Mask slice = mask.extractSlice(z);
                Channel distanceSlice =
                        createDistanceMapFromPlugin(
                                slice, true, multiplyBy, multiplyByZRes, createShort, applyResolution);
                channelOut.voxels().transferSlice(z, distanceSlice.voxels(), 0, true);
            }

            return channelOut;

        } else {
            return createDistanceMapFromPlugin(
                    mask, false, multiplyBy, multiplyByZRes, createShort, applyResolution);
        }
    }
        
    private static boolean hasNanZResolution(Optional<Resolution> resolution) {
        if (resolution.isPresent()) {
              return Double.isNaN(resolution.get().z());
        } else {
              return false;
        }
    }

    private static Channel createEmptyChannel(boolean createShort, Dimensions dims) {
        VoxelDataType dataType =
                createShort ? UnsignedShortVoxelType.INSTANCE : UnsignedByteVoxelType.INSTANCE;
        return ChannelFactory.instance().createUninitialised(dims, dataType);
    }
    
    private static Channel createDistanceMapFromPlugin(
            Mask mask,
            boolean suppressZ,
            float multFactor,
            float multFactorZ,
            boolean createShort,
            boolean applyResolution) {
        
        float[] multipliers = new float[]{
            multiplicationFactor(multFactor, applyResolution, mask, Resolution::x),
            multiplicationFactor(multFactor, applyResolution, mask, Resolution::y),
            multiplicationFactor(multFactorZ, applyResolution, mask, Resolution::z)
        };
        
        Channel distanceAsFloat =
                EDT.compute(
                        mask,
                        ChannelFactory.instance().get(FloatVoxelType.INSTANCE),
                        suppressZ,
                        multipliers);

        ChannelConverter<?> converter = createShort ? new ToUnsignedShort() : new ToUnsignedByte();
        return converter.convert(distanceAsFloat, ConversionPolicy.CHANGE_EXISTING_CHANNEL);
    }

    private void checkZResolution(Resolution resolution) throws ProvisionFailedException {
        double zRelRes = resolution.zRelative();
        if (!ignoreZIfNaN && Double.isNaN(zRelRes)) {
            throw new ProvisionFailedException("Z-resolution is NaN");
        }

        if (zRelRes == 0) {
            throw new ProvisionFailedException("Z-resolution is 0");
        }
    }

    private static float multiplicationFactor(
            float multFactor, boolean applyResolution, Mask mask, ToDoubleFunction<Resolution> extractFromResolution) {
        if (applyResolution && mask.resolution().isPresent()) {
            return (float)(multFactor * extractFromResolution.applyAsDouble(mask.resolution().get()));   // NOSONAR
        } else {
            return multFactor;
        }
    }
}
