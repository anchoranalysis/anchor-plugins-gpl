package anchor.fiji.bean.define.adder;

/*-
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2016 - 2020 ETH Zurich, University of Zurich, Owen Feehan
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.anchoranalysis.image.bean.provider.BinaryChnlProvider;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.bean.provider.ImageDimProvider;
import org.anchoranalysis.image.bean.provider.ObjMaskProvider;
import org.anchoranalysis.image.bean.unitvalue.distance.UnitValueDistance;
import org.anchoranalysis.image.bean.unitvalue.volume.UnitValueVolume;
import org.anchoranalysis.plugin.ml.bean.cluster.ObjMaskProviderMergeDBScan;

import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderDistanceTransformExact3D;
import ch.ethz.biol.cell.imageprocessing.chnl.provider.ChnlProviderSubtractFromConstant;
import ch.ethz.biol.cell.imageprocessing.dim.provider.ImageDimProviderFromChnl;
import ch.ethz.biol.cell.imageprocessing.objmask.provider.ObjMaskProviderConnectedComponents;
import ch.ethz.biol.cell.imageprocessing.objmask.provider.ObjMaskProviderConvexHullConnectLines;


/**
 * Beans related to non-segmentation
 * 
 * @author FEEHANO
 *
 */
class FactoryOther {

	public static ObjMaskProvider connectedComponentsInput(
		BinaryChnlProvider source,
		UnitValueVolume minVolumeConnectedComponent
	) {
		ObjMaskProviderConnectedComponents provider = new ObjMaskProviderConnectedComponents();
		provider.setMinVolume(minVolumeConnectedComponent);
		provider.setBinaryChnl( source );
		return provider;
	}
	
	public static ImageDimProvider dimsFromChnl( ChnlProvider chnlProvider ) {
		ImageDimProviderFromChnl provider = new ImageDimProviderFromChnl();
		provider.setChnl(chnlProvider);
		return provider;
	}
	
	public static ChnlProvider distanceTransformBeforeInvert(
		BinaryChnlProvider source,
		double distanceTransformMultiplyBy
	) {
		ChnlProviderDistanceTransformExact3D provider = new ChnlProviderDistanceTransformExact3D();
		provider.setCreateShort(true);
		provider.setMultiplyBy(distanceTransformMultiplyBy);
		provider.setSuppressZ(true);
		provider.setMask(source);
		return provider;
	}
	
	public static ChnlProvider distanceTransformAfterInvert( ChnlProvider source ) {
		ChnlProviderSubtractFromConstant provider = new ChnlProviderSubtractFromConstant();
		provider.setValue(65535);
		provider.setChnl(source);
		return provider;
	}
	
	public static ObjMaskProvider mergeMinima(
		ObjMaskProvider unmergedMinima,
		ObjMaskProvider container,
		ImageDimProvider resProvider,
		ChnlProvider sourceDistanceMapProvider,
		UnitValueDistance maxDistanceCOG,
		double maxDistDeltaContour
	) {
		ObjMaskProviderMergeDBScan merge = new ObjMaskProviderMergeDBScan(); 
		merge.setObjs(unmergedMinima);
		merge.setObjsContainer(container);
		merge.setResProvider(resProvider);
		merge.setDistanceMapProvider(sourceDistanceMapProvider);
		merge.setMaxDistCOG(maxDistanceCOG);
		merge.setMaxDistDeltaContour(maxDistDeltaContour);
		return merge;
	}
	
	public static ObjMaskProvider seeds( ObjMaskProvider mergedMinima, ImageDimProvider dimProvider ) {
		ObjMaskProviderConvexHullConnectLines provider = new ObjMaskProviderConvexHullConnectLines();
		provider.setObjs(mergedMinima);
		provider.setDim(dimProvider);
		return provider;
	}
	
}
