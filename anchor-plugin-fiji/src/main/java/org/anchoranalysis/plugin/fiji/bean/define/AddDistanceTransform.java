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

import static org.anchoranalysis.plugin.fiji.bean.define.FactoryOther.*;
import static org.anchoranalysis.plugin.fiji.bean.define.SegmentFactory.*;

import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.define.Define;
import org.anchoranalysis.bean.define.DefineAddException;
import org.anchoranalysis.bean.define.adder.DefineAdderWithPrefixBean;
import org.anchoranalysis.image.bean.provider.ChannelProvider;
import org.anchoranalysis.image.bean.provider.DimensionsProvider;
import org.anchoranalysis.image.bean.provider.MaskProvider;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.bean.unitvalue.extent.volume.UnitValueVolume;
import org.anchoranalysis.image.bean.unitvalue.extent.volume.VolumeVoxels;
import org.anchoranalysis.plugin.image.bean.blur.BlurGaussian3D;
import org.anchoranalysis.plugin.image.bean.blur.BlurStrategy;
import org.anchoranalysis.plugin.image.bean.channel.provider.Duplicate;
import org.anchoranalysis.plugin.image.bean.channel.provider.intensity.Blur;
import org.anchoranalysis.plugin.image.provider.ReferenceFactory;

/**
 * Performs a <a href="https://en.wikipedia.org/wiki/Watershed_(image_processing)">Watershed</a> on
 * <a href="https://en.wikipedia.org/wiki/Distance_transform">distance transform</a> by bundling
 * together several other beans.
 *
 * <p>The distance transform is Euclidean.
 *
 * <p>This is used to avoid repetitive bean-definitions in Define, but while still providing
 * visualization of all the intermediate steps that occur during the transformation, which are
 * typically vital for visualization.
 *
 * <p>For now, it only works in 2D, but can be easily extended for 3D.
 *
 * <p>The steps are:
 *
 * <ol>
 *   <li>Find connected components of a binary mask.
 *   <li>Find the distance transform of the above.
 *   <li>Invert the distance transform.
 *   <li>Find minima points from the above.
 *   <li>Merge minima points that occur within a certain distance (they become the same object, but
 *       it can be disconnected voxelwise).
 *   <li>Create seeds by drawing a line between the merged-minima-points so they are now connected.
 *   <li>Apply the watershed transformation using the seeds to create segments.
 * </ol>
 *
 * @author Owen Feehan
 */
public class AddDistanceTransform extends DefineAdderWithPrefixBean {

    private static final String CONNECTED_INPUT = "objsInputConnected";
    private static final String DISTANCE_TRANSFORM = "channelDistance";
    private static final String DISTANCE_TRANSFORM_SMOOTH = "channelDistanceSmooth";
    private static final String DISTANCE_TRANSFORM_BEFORE_INVERT = "channelDistanceBeforeInvert";
    private static final String MINIMA_UNMERGED =
            "objsMinimaUnmerged"; // Minima have not yet been 'merged' together.
    private static final String MINIMA_MERGED =
            "objsMinimaMerged"; // Minima are merged, but can still be disconnected.
    private static final String SEEDS = "objsSeeds";
    private static final String SEGMENTS = "objsSegments";

    // START BEAN PROPERTIES
    /** The ID of the binary input mask that determines the region of the watershed. */
    @BeanField @Getter @Setter private String binaryInputChannelID;

    @BeanField @Getter @Setter
    private UnitValueVolume minVolumeConnectedComponent = new VolumeVoxels(1);

    /** Multiplies the distance transform by this factor to make it more meaningful in a short. */
    @BeanField @Getter @Setter private float distanceTransformMultiplyBy = 1.0f;

    /**
     * If non-zero, a Gaussian blur is applied to the distance transform using the sigma in meters
     * below.
     */
    @BeanField @Getter @Setter private double distanceTransformSmoothSigmaMeters = 0;

    @BeanField @Getter @Setter private UnitValueDistance maxDistanceBetweenMinima;

    /**
     * The maximum distance allowed between two seeds in terms of their values in the distance map.
     */
    @BeanField @Getter @Setter private double maxDistanceDeltaContour = Double.MAX_VALUE;
    // END BEAN PROPERTIES

    @Override
    public void addTo(Define define) throws DefineAddException {

        // Step 1
        addConnectedInput(define);

        // Steps 2-3
        addDistanceTransform(define);

        // Steps 4-6
        addSeedFinding(define);

        // Step 7
        addSegments(define);
    }

    private void addSegments(Define define) throws DefineAddException {
        addWithName(
                define,
                SEGMENTS,
                watershedSegment(
                        objects(CONNECTED_INPUT), objects(SEEDS), channel(DISTANCE_TRANSFORM)));
    }

    private void addConnectedInput(Define define) throws DefineAddException {
        addWithName(
                define,
                CONNECTED_INPUT,
                connectedComponentsInput(inputMask(), minVolumeConnectedComponent));
    }

    private void addDistanceTransform(Define define) throws DefineAddException {

        addWithName(
                define,
                DISTANCE_TRANSFORM_BEFORE_INVERT,
                distanceTransformBeforeInvert(inputMask(), distanceTransformMultiplyBy));

        if (isDistanceTransformSmoothed()) {
            addWithName(
                    define,
                    DISTANCE_TRANSFORM_SMOOTH,
                    smooth(
                            duplicateChannel(DISTANCE_TRANSFORM_BEFORE_INVERT),
                            distanceTransformSmoothSigmaMeters));
        }

        addWithName(
                define,
                DISTANCE_TRANSFORM,
                distanceTransformAfterInvert(duplicateChannel(sourceForInversion())));
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

    private static ChannelProvider smooth(
            ChannelProvider src, double distanceTransformSmoothedSigmaMeters) {
        Blur provider = new Blur();
        provider.setStrategy(createBlurStrategy(distanceTransformSmoothedSigmaMeters));
        provider.setChannel(src);
        return provider;
    }

    private static BlurStrategy createBlurStrategy(double distanceTransformSmoothedSigmaMeters) {
        BlurGaussian3D blurStrategy = new BlurGaussian3D();
        blurStrategy.setSigma(distanceTransformSmoothedSigmaMeters);
        blurStrategy.setSigmaInMeters(true);
        return blurStrategy;
    }

    private void addSeedFinding(Define define) throws DefineAddException {
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

        addWithName(define, SEEDS, seeds(objects(MINIMA_MERGED)));
    }

    private ChannelProvider channel(String unresolvedID) {
        return ReferenceFactory.channel(resolveName(unresolvedID));
    }

    private DimensionsProvider dimensions() {
        return dimensionsFromChannel(channel(DISTANCE_TRANSFORM));
    }

    private ChannelProvider duplicateChannel(String unresolvedID) {
        Duplicate dup = new Duplicate();
        dup.setChannel(channel(unresolvedID));
        return dup;
    }

    private ObjectCollectionProvider objects(String unresolvedID) {
        return ReferenceFactory.objects(resolveName(unresolvedID));
    }

    private MaskProvider inputMask() {
        return ReferenceFactory.mask(binaryInputChannelID);
    }
}