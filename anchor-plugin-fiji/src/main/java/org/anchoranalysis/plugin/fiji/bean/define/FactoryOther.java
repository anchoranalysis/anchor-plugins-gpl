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
package org.anchoranalysis.plugin.fiji.bean.define;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.anchoranalysis.image.bean.provider.ChannelProvider;
import org.anchoranalysis.image.bean.provider.DimensionsProvider;
import org.anchoranalysis.image.bean.provider.MaskProvider;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.bean.unitvalue.extent.volume.UnitValueVolume;
import org.anchoranalysis.plugin.fiji.bean.channel.provider.distance.DistanceTransform3D;
import org.anchoranalysis.plugin.image.bean.channel.provider.arithmetic.SubtractFromConstant;
import org.anchoranalysis.plugin.image.bean.dimensions.provider.FromChannel;
import org.anchoranalysis.plugin.image.bean.object.provider.connected.ConnectedComponentsFromMask;
import org.anchoranalysis.plugin.imagej.bean.object.provider.DrawLineAlongConvexHull;
import org.anchoranalysis.plugin.ml.bean.cluster.MergeSpatialClusters;

/**
 * Beans related to non-segmentation
 *
 * @author Owen Feehan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FactoryOther {

    public static ObjectCollectionProvider connectedComponentsInput(
            MaskProvider source, UnitValueVolume minVolumeConnectedComponent) {
        ConnectedComponentsFromMask provider = new ConnectedComponentsFromMask();
        provider.setMinVolume(minVolumeConnectedComponent);
        provider.setMask(source);
        return provider;
    }

    public static DimensionsProvider dimensionsFromChannel(ChannelProvider channelProvider) {
        FromChannel provider = new FromChannel();
        provider.setChannel(channelProvider);
        return provider;
    }

    public static ChannelProvider distanceTransformBeforeInvert(
            MaskProvider source, float distanceTransformMultiplyBy) {
        DistanceTransform3D provider = new DistanceTransform3D();
        provider.setCreateShort(true);
        provider.setMultiplyBy(distanceTransformMultiplyBy);
        provider.setSuppressZ(true);
        provider.setMask(source);
        return provider;
    }

    public static ChannelProvider distanceTransformAfterInvert(ChannelProvider source) {
        SubtractFromConstant provider = new SubtractFromConstant();
        provider.setValue(65535);
        provider.setChannel(source);
        return provider;
    }

    public static ObjectCollectionProvider mergeMinima(
            ObjectCollectionProvider unmergedMinima,
            ObjectCollectionProvider container,
            DimensionsProvider dimensionsProvider,
            ChannelProvider sourceDistanceMapProvider,
            UnitValueDistance maxDistanceCOG,
            double maxDistanceDeltaContour) {
        MergeSpatialClusters merge = new MergeSpatialClusters();
        merge.setObjects(unmergedMinima);
        merge.setObjectsContainer(container);
        merge.setDimensions(dimensionsProvider);
        merge.setDistanceMapProvider(sourceDistanceMapProvider);
        merge.setMaxDistanceCOG(maxDistanceCOG);
        merge.setMaxDistanceDeltaContour(maxDistanceDeltaContour);
        return merge;
    }

    public static ObjectCollectionProvider seeds(ObjectCollectionProvider mergedMinima) {
        DrawLineAlongConvexHull provider = new DrawLineAlongConvexHull();
        provider.setObjects(mergedMinima);
        return provider;
    }
}
