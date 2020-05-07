package anchor.fiji.bean.define.adder;

import org.anchoranalysis.image.bean.provider.BinaryImgChnlProvider;
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
		BinaryImgChnlProvider source,
		UnitValueVolume minVolumeConnectedComponent
	) {
		ObjMaskProviderConnectedComponents provider = new ObjMaskProviderConnectedComponents();
		provider.setMinVolume(minVolumeConnectedComponent);
		provider.setBinaryImgChnlProvider( source );
		return provider;
	}
	
	public static ImageDimProvider dimsFromChnl( ChnlProvider chnlProvider ) {
		ImageDimProviderFromChnl provider = new ImageDimProviderFromChnl();
		provider.setChnlProvider(chnlProvider);
		return provider;
	}
	
	public static ChnlProvider distanceTransformBeforeInvert(
		BinaryImgChnlProvider source,
		double distanceTransformMultiplyBy
	) {
		ChnlProviderDistanceTransformExact3D provider = new ChnlProviderDistanceTransformExact3D();
		provider.setCreateShort(true);
		provider.setMultiplyBy(distanceTransformMultiplyBy);
		provider.setSuppressZ(true);
		provider.setBinaryImgChnlProvider( source );
		return provider;
	}
	
	public static ChnlProvider distanceTransformAfterInvert( ChnlProvider source ) {
		ChnlProviderSubtractFromConstant provider = new ChnlProviderSubtractFromConstant();
		provider.setValue(65535);
		provider.setChnlProvider(source);
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
		provider.setDimProvider(dimProvider);
		return provider;
	}
	
}
