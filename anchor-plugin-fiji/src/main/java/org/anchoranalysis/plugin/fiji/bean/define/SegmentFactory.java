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
import org.anchoranalysis.image.bean.provider.MaskProvider;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.bean.segment.object.SegmentChannelIntoObjects;
import org.anchoranalysis.plugin.image.bean.object.provider.segment.SegmentChannel;
import org.anchoranalysis.plugin.image.bean.object.provider.segment.SegmentWithSeeds;
import org.anchoranalysis.plugin.image.bean.object.segment.channel.ImposeMinima;
import org.anchoranalysis.plugin.image.bean.object.segment.channel.watershed.minima.MinimaImpositionGrayscaleReconstruction;
import org.anchoranalysis.plugin.image.bean.object.segment.channel.watershed.minima.grayscalereconstruction.GrayscaleReconstructionRobinson;
import org.anchoranalysis.plugin.image.bean.object.segment.channel.watershed.yeong.WatershedYeong;

/**
 * Provides segmentations wrapped in a {@link ObjectCollectionProvider}.
 *
 * @author Owen Feehan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class SegmentFactory {

    public static ObjectCollectionProvider minimaUnmerged(
            MaskProvider mask, ChannelProvider distanceTransform) {
        SegmentChannel provider = new SegmentChannel();
        provider.setMask(mask);
        provider.setChannel(distanceTransform);
        provider.setSegment(watershedForMinima());
        return provider;
    }

    public static ObjectCollectionProvider watershedSegment(
            ObjectCollectionProvider sourceObjects,
            ObjectCollectionProvider seeds,
            ChannelProvider distanceTransform) {
        SegmentWithSeeds provider = new SegmentWithSeeds();
        provider.setObjectsSource(sourceObjects);
        provider.setObjectsSeeds(seeds);
        provider.setChannel(distanceTransform);
        provider.setSegment(watershedForSegments());
        return provider;
    }

    private static SegmentChannelIntoObjects watershedForMinima() {
        WatershedYeong segment = new WatershedYeong();
        segment.setExitWithMinima(true);
        return segment;
    }

    private static SegmentChannelIntoObjects watershedForSegments() {
        ImposeMinima impose = new ImposeMinima();
        impose.setSegment(new WatershedYeong());
        impose.setMinimaImposition(minimaImposion());
        return impose;
    }

    private static MinimaImpositionGrayscaleReconstruction minimaImposion() {
        MinimaImpositionGrayscaleReconstruction impose =
                new MinimaImpositionGrayscaleReconstruction();
        impose.setGrayscaleReconstruction(new GrayscaleReconstructionRobinson());
        return impose;
    }
}
