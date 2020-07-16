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


import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderDistanceTransformExact3D;
import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderSubtractFromConstant;
import ch.ethz.biol.cell.imageprocessing.dim.provider.ImageDimProviderFromChnl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.anchoranalysis.image.bean.provider.BinaryChnlProvider;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.bean.provider.ImageDimProvider;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.bean.unitvalue.volume.UnitValueVolume;
import org.anchoranalysis.plugin.ij.bean.object.provider.DrawLineAlongConvexHull;
import org.anchoranalysis.plugin.image.bean.object.provider.connected.ConnectedComponentsFromMask;
import org.anchoranalysis.plugin.ml.bean.object.provider.MergeSpatialClusters;

/**
 * Beans related to non-segmentation
 *
 * @author Owen Feehan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FactoryOther {

    public static ObjectCollectionProvider connectedComponentsInput(
            BinaryChnlProvider source, UnitValueVolume minVolumeConnectedComponent) {
        ConnectedComponentsFromMask provider = new ConnectedComponentsFromMask();
        provider.setMinVolume(minVolumeConnectedComponent);
        provider.setBinaryChnl(source);
        return provider;
    }

    public static ImageDimProvider dimsFromChnl(ChnlProvider chnlProvider) {
        ImageDimProviderFromChnl provider = new ImageDimProviderFromChnl();
        provider.setChnl(chnlProvider);
        return provider;
    }

    public static ChnlProvider distanceTransformBeforeInvert(
            BinaryChnlProvider source, double distanceTransformMultiplyBy) {
        ChnlProviderDistanceTransformExact3D provider = new ChnlProviderDistanceTransformExact3D();
        provider.setCreateShort(true);
        provider.setMultiplyBy(distanceTransformMultiplyBy);
        provider.setSuppressZ(true);
        provider.setMask(source);
        return provider;
    }

    public static ChnlProvider distanceTransformAfterInvert(ChnlProvider source) {
        ChnlProviderSubtractFromConstant provider = new ChnlProviderSubtractFromConstant();
        provider.setValue(65535);
        provider.setChnl(source);
        return provider;
    }

    public static ObjectCollectionProvider mergeMinima(
            ObjectCollectionProvider unmergedMinima,
            ObjectCollectionProvider container,
            ImageDimProvider resProvider,
            ChnlProvider sourceDistanceMapProvider,
            UnitValueDistance maxDistanceCOG,
            double maxDistanceDeltaContour) {
        MergeSpatialClusters merge = new MergeSpatialClusters();
        merge.setObjects(unmergedMinima);
        merge.setObjectsContainer(container);
        merge.setDim(resProvider);
        merge.setDistanceMapProvider(sourceDistanceMapProvider);
        merge.setMaxDistanceCOG(maxDistanceCOG);
        merge.setMaxDistanceDeltaContour(maxDistanceDeltaContour);
        return merge;
    }

    public static ObjectCollectionProvider seeds(
            ObjectCollectionProvider mergedMinima, ImageDimProvider dimProvider) {
        DrawLineAlongConvexHull provider = new DrawLineAlongConvexHull();
        provider.setObjects(mergedMinima);
        provider.setDim(dimProvider);
        return provider;
    }
}
