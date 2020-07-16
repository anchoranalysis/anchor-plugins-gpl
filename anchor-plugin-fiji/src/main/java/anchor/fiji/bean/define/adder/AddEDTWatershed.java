package anchor.fiji.bean.define.adder;

import static anchor.fiji.bean.define.adder.FactoryOther.*;
import static anchor.fiji.bean.define.adder.FactorySgmn.*;

import ch.ethz.biol.cell.imageprocessing.binaryimgchnl.provider.BinaryChnlProviderReference;
import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderBlur;
import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderDuplicate;
import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderReference;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.define.Define;
import org.anchoranalysis.bean.define.adder.DefineAdderWithPrefixBean;
import org.anchoranalysis.bean.xml.error.BeanXmlException;
import org.anchoranalysis.image.bean.provider.BinaryChnlProvider;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.bean.provider.ImageDimProvider;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.bean.unitvalue.volume.UnitValueVolume;
import org.anchoranalysis.image.bean.unitvalue.volume.UnitValueVolumeVoxels;
import org.anchoranalysis.plugin.image.bean.blur.BlurGaussian3D;
import org.anchoranalysis.plugin.image.bean.blur.BlurStrategy;
import org.anchoranalysis.plugin.image.bean.object.provider.Reference;

/**
 * Performs a Watershed on an EDT transform by bundling together several other beans
 *
 * <p>This is used to avoid repetitive bean-definitions in Define, but while still providing
 * visualization of all the intermediate steps that occur during the transformation, which are
 * typically vital for visualization.
 *
 * <p>For now, it only works in 2D, but can be easily extended for 3D.
 *
 * <p>The steps are: 1. Find connected components of a binary mask 2. Find the distance transform of
 * the above 3. Invert the distance transform 4. Find minima points from the above 5. Merge minima
 * points that occur within a certain distance (they become the same object, but it can be
 * disconnected voxelwise) 6. Create seeds by drawing a line between the merged-minima-points so
 * they are now connected. 7. Apply the watershed transformation using the seeds to create segments
 *
 * @author Owen Feehan
 */
public class AddEDTWatershed extends DefineAdderWithPrefixBean {

    private static final String CONNECTED_INPUT = "objsInputConnected";
    private static final String DISTANCE_TRANSFORM = "chnlDistance";
    private static final String DISTANCE_TRANSFORM_SMOOTH = "chnlDistanceSmooth";
    private static final String DISTANCE_TRANSFORM_BEFORE_INVERT = "chnlDistanceBeforeInvert";
    private static final String MINIMA_UNMERGED =
            "objsMinimaUnmerged"; // Minima have not yet been 'merged' together
    private static final String MINIMA_MERGED =
            "objsMinimaMerged"; // Minima are merged, but can still be disconnected
    private static final String SEEDS = "objsSeeds";
    private static final String SEGMENTS = "objsSegments";

    // START BEAN PROPERTIES
    /** The ID of the binary input mask that determines the region of the watershed */
    @BeanField @Getter @Setter private String binaryInputChnlID;

    @BeanField @Getter @Setter
    private UnitValueVolume minVolumeConnectedComponent = new UnitValueVolumeVoxels(1);

    /** Multiplies the distance transform by this factor to make it more meaningful in a short */
    @BeanField @Getter @Setter private double distanceTransformMultiplyBy = 1.0;

    /**
     * If non-zero, a Gaussian blur is applied to the distance transform using the sigma in meters
     * below
     */
    @BeanField @Getter @Setter private double distanceTransformSmoothSigmaMeters = 0;

    @BeanField @Getter @Setter private UnitValueDistance maxDistanceBetweenMinima;

    /**
     * The maximum distance allowed between two seeds in terms of their values in the distance map
     */
    @BeanField @Getter @Setter private double maxDistanceDeltaContour = Double.MAX_VALUE;
    // END BEAN PROPERTIES

    @Override
    public void addTo(Define define) throws BeanXmlException {

        // Step 1
        addConnectedInput(define);

        // Steps 2-3
        addDistanceTransform(define);

        // Steps 4-6
        addSeedFinding(define);

        // Step 7
        addSegments(define);
    }

    private void addSegments(Define define) throws BeanXmlException {
        addWithName(
                define,
                SEGMENTS,
                watershedSegment(
                        objects(CONNECTED_INPUT), objects(SEEDS), channel(DISTANCE_TRANSFORM)));
    }

    private void addConnectedInput(Define define) throws BeanXmlException {
        addWithName(
                define,
                CONNECTED_INPUT,
                connectedComponentsInput(inputMask(), minVolumeConnectedComponent));
    }

    private void addDistanceTransform(Define define) throws BeanXmlException {

        addWithName(
                define,
                DISTANCE_TRANSFORM_BEFORE_INVERT,
                distanceTransformBeforeInvert(inputMask(), distanceTransformMultiplyBy));

        if (isDistanceTransformSmoothed()) {
            addWithName(
                    define,
                    DISTANCE_TRANSFORM_SMOOTH,
                    smooth(
                            duplicateChnl(DISTANCE_TRANSFORM_BEFORE_INVERT),
                            distanceTransformSmoothSigmaMeters));
        }

        addWithName(
                define,
                DISTANCE_TRANSFORM,
                distanceTransformAfterInvert(duplicateChnl(sourceForInversion())));
    }

    private boolean isDistanceTransformSmoothed() {
        return distanceTransformSmoothSigmaMeters > 0;
    }

    private String sourceForInversion() {
        if (isDistanceTransformSmoothed()) {
            return DISTANCE_TRANSFORM_SMOOTH;
        } else {
            return DISTANCE_TRANSFORM_BEFORE_INVERT;
        }
    }

    private static ChnlProvider smooth(
            ChnlProvider src, double distanceTransformSmoothedSigmaMeters) {
        ChnlProviderBlur provider = new ChnlProviderBlur();
        provider.setStrategy(createBlurStrategy(distanceTransformSmoothedSigmaMeters));
        provider.setChnl(src);
        return provider;
    }

    private static BlurStrategy createBlurStrategy(double distanceTransformSmoothedSigmaMeters) {
        BlurGaussian3D blurStrategy = new BlurGaussian3D();
        blurStrategy.setSigma(distanceTransformSmoothedSigmaMeters);
        blurStrategy.setSigmaInMeters(true);
        return blurStrategy;
    }

    private void addSeedFinding(Define define) throws BeanXmlException {
        addWithName(
                define, MINIMA_UNMERGED, minimaUnmerged(inputMask(), channel(DISTANCE_TRANSFORM)));

        addWithName(
                define,
                MINIMA_MERGED,
                mergeMinima(
                        objects(MINIMA_UNMERGED),
                        objects(CONNECTED_INPUT),
                        dimensions(),
                        channel(DISTANCE_TRANSFORM),
                        maxDistanceBetweenMinima,
                        maxDistanceDeltaContour));

        addWithName(define, SEEDS, seeds(objects(MINIMA_MERGED), dimensions()));
    }

    private ChnlProvider channel(String unresolvedID) {
        return new ChnlProviderReference(resolveName(unresolvedID));
    }

    private ImageDimProvider dimensions() {
        return dimsFromChnl(channel(DISTANCE_TRANSFORM));
    }

    private ChnlProvider duplicateChnl(String unresolvedID) {
        ChnlProviderDuplicate dup = new ChnlProviderDuplicate();
        dup.setChnl(channel(unresolvedID));
        return dup;
    }

    private ObjectCollectionProvider objects(String unresolvedID) {
        return new Reference(resolveName(unresolvedID));
    }

    private BinaryChnlProvider inputMask() {
        return new BinaryChnlProviderReference(binaryInputChnlID);
    }
}
