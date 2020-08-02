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
package anchor.fiji.bean.define.adder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.anchoranalysis.image.bean.provider.ChannelProvider;
import org.anchoranalysis.image.bean.provider.MaskProvider;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.bean.segment.object.SegmentChannelIntoObjects;
import org.anchoranalysis.plugin.image.bean.object.provider.segment.SegmentChannel;
import org.anchoranalysis.plugin.image.bean.object.provider.segment.SegmentWithSeeds;
import org.anchoranalysis.plugin.image.bean.object.segment.ImposeMinima;
import org.anchoranalysis.plugin.image.bean.object.segment.watershed.minima.MinimaImpositionGrayscaleReconstruction;
import org.anchoranalysis.plugin.image.bean.object.segment.watershed.minima.grayscalereconstruction.GrayscaleReconstructionRobinson;
import org.anchoranalysis.plugin.image.bean.object.segment.watershed.yeong.WatershedYeong;

/**
 * Beans related to segmentation
 *
 * @author Owen Feehan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FactorySgmn {

    public static ObjectCollectionProvider minimaUnmerged(
            MaskProvider mask, ChannelProvider distanceTransform) {
        SegmentChannel provider = new SegmentChannel();
        provider.setMask(mask);
        provider.setChnl(distanceTransform);
        provider.setSgmn(watershedSgmnForMinima());
        return provider;
    }

    public static ObjectCollectionProvider watershedSegment(
            ObjectCollectionProvider sourceObjects,
            ObjectCollectionProvider seeds,
            ChannelProvider distanceTransform) {
        SegmentWithSeeds provider = new SegmentWithSeeds();
        provider.setObjectsSource(sourceObjects);
        provider.setObjectsSeeds(seeds);
        provider.setChnl(distanceTransform);
        provider.setSgmn(watershedSgmnForSegments());
        return provider;
    }

    private static SegmentChannelIntoObjects watershedSgmnForMinima() {
        WatershedYeong sgmn = new WatershedYeong();
        sgmn.setExitWithMinima(true);
        return sgmn;
    }

    private static SegmentChannelIntoObjects watershedSgmnForSegments() {

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
