package anchor.fiji.bean.define.adder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.anchoranalysis.image.bean.provider.BinaryChnlProvider;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
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
            BinaryChnlProvider mask, ChnlProvider distanceTransform) {
        SegmentChannel provider = new SegmentChannel();
        provider.setMask(mask);
        provider.setChnl(distanceTransform);
        provider.setSgmn(watershedSgmnForMinima());
        return provider;
    }

    public static ObjectCollectionProvider watershedSegment(
            ObjectCollectionProvider sourceObjects,
            ObjectCollectionProvider seeds,
            ChnlProvider distanceTransform) {
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
