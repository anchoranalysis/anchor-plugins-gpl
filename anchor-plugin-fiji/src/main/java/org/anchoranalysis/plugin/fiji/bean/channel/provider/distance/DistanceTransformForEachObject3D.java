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

import lombok.Getter;
import lombok.Setter;
import java.util.Optional;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactory;
import org.anchoranalysis.image.convert.UnsignedByteBuffer;
import org.anchoranalysis.image.extent.Dimensions;
import org.anchoranalysis.image.extent.Resolution;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.voxel.Voxels;
import org.anchoranalysis.image.voxel.datatype.UnsignedByteVoxelType;
import org.anchoranalysis.plugin.image.bean.channel.provider.FromDimensionsBase;

/**
 * Like {@link DistanceTransform3D} but applies the distance transform separately for each object in
 * a collection.
 *
 * <p>A new channel is always created i.e. the input channel is unchanged.
 *
 * @author Owen Feehan
 */
public class DistanceTransformForEachObject3D extends FromDimensionsBase {

    // START PROPERTIES
    @BeanField @Getter @Setter private ObjectCollectionProvider objects;

    @BeanField @Getter @Setter private boolean suppressZ = false;

    @BeanField @Getter @Setter private boolean createShort = false;
    // END PROPERTIES

    @Override
    protected Channel createFromDimensions(Dimensions dimensions) throws CreateException {

        Channel out = ChannelFactory.instance().create(dimensions, UnsignedByteVoxelType.INSTANCE);

        Voxels<UnsignedByteBuffer> voxelsOut = out.voxels().asByte();

        for (ObjectMask object : objects.create()) {
            copyObjectToOutput(object, dimensions.resolution(), voxelsOut);
        }

        return out;
    }

    /**
     * Performs a distance-transform on an individual object, and copies the output to voxels for
     * all objects.
     *
     * @param object the object to copy
     * @param resolution the image-resolution
     * @param destination voxels into which
     * @throws CreateException
     */
    private void copyObjectToOutput(
            ObjectMask object, Optional<Resolution> resolution, Voxels<UnsignedByteBuffer> destination)
            throws CreateException {

        Voxels<UnsignedByteBuffer> voxelsDistance = distanceTransformForObject(object, resolution);

        ObjectMask objectAtOrigin = object.shiftToOrigin();

        voxelsDistance.extract().objectCopyTo(objectAtOrigin, destination, object.boundingBox());
    }

    private Voxels<UnsignedByteBuffer> distanceTransformForObject(
            ObjectMask object, Optional<Resolution> resolution) throws CreateException {
        return DistanceTransform3D.createDistanceMapForVoxels(
                object.binaryVoxels()
                        .duplicate(), // TODO duplicated presumably because the voxel-buffer is
                // consumed?
                resolution,
                suppressZ,
                1.0,
                1.0,
                createShort,
                false);
    }
}
