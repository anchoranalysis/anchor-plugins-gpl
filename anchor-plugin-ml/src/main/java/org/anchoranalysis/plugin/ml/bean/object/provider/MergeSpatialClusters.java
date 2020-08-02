/*-
 * #%L
 * anchor-plugin-ml
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
package org.anchoranalysis.plugin.ml.bean.object.provider;


import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.bean.provider.ChannelProvider;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.object.ObjectCollection;
import org.anchoranalysis.image.object.ObjectCollectionFactory;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.object.ops.ObjectMaskMerger;
import org.anchoranalysis.plugin.image.bean.object.provider.merge.MergeBase;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;

/**
 * Merges spatially-adjacent objects using the DBScan clustering algorithm
 *
 * <p>The features passed to DBScanare based on:
 *
 * <ul>
 *   <li>Euclidian distance
 *   <li>an @{code eps} (max distance for neighborhood connection) where distance is less than
 *       {@code maxDistanceCOG} (calculated on XY resolution)
 * </ul>
 *
 * @author Owen Feehan
 */
public class MergeSpatialClusters extends MergeBase {

    // START BEAN PROPERTIES
    /** A distance map which can also be used for making decisions on merging */
    @BeanField @Getter @Setter private ChannelProvider distanceMapProvider;

    /** The maximum distance allowed between center-of-gravities of objects */
    @BeanField @Getter @Setter
    private UnitValueDistance
            maxDistanceCOG; // provides a maximum distance for a single step (eps). This is resolved
    // in XY plane (assuming isotropy)

    /**
     * The maximum distance allowed between the 'distance from contour' values provided from the
     * distanceMap for each point.
     */
    @BeanField @Getter @Setter private double maxDistanceDeltaContour = Double.MAX_VALUE;
    // END BEAN PROPERTIES

    @Override
    public ObjectCollection createFromObjects(ObjectCollection objectsToMerge)
            throws CreateException {

        try {
            return mergeMultiplex(objectsToMerge, this::clusterAndMerge);
        } catch (OperationFailedException e) {
            throw new CreateException(e);
        }
    }

    private ObjectCollection clusterAndMerge(ObjectCollection objects)
            throws OperationFailedException {

        DBSCANClusterer<ObjectMaskPlus> clusterer =
                new DBSCANClusterer<>(
                        1.0, // Maximum distance allowed to merge points
                        0, // Ensures no object is discarded as "noise"
                        new DistanceCogDistanceMapMeasure(
                                calcResRequired(), maxDistanceCOG, maxDistanceDeltaContour));

        try {
            Channel distanceMap = distanceMapProvider.create();

            List<Cluster<ObjectMaskPlus>> clusters =
                    clusterer.cluster(convert(objects, distanceMap));
            return mergeClusters(clusters);

        } catch (CreateException e) {
            throw new OperationFailedException(e);
        }
    }

    private Collection<ObjectMaskPlus> convert(ObjectCollection objects, Channel distanceMap) {
        return objects.stream()
                .mapToList(objectMask -> new ObjectMaskPlus(objectMask, distanceMap, getLogger()));
    }

    private static ObjectCollection mergeClusters(List<Cluster<ObjectMaskPlus>> clusters)
            throws OperationFailedException {
        return ObjectCollectionFactory.mapFrom(clusters, MergeSpatialClusters::mergeCluster);
    }

    private static ObjectMask mergeCluster(Cluster<ObjectMaskPlus> cluster)
            throws OperationFailedException {
        return ObjectMaskMerger.merge(convert(cluster.getPoints()));
    }

    private static ObjectCollection convert(Collection<ObjectMaskPlus> objects) {
        return ObjectCollectionFactory.mapFrom(objects, ObjectMaskPlus::getObject);
    }
}
