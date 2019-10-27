package anchor.fiji.bean.define.adder;

import org.anchoranalysis.image.bean.provider.BinaryImgChnlProvider;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.bean.provider.ObjMaskProvider;
import org.anchoranalysis.image.bean.sgmn.objmask.ObjMaskSgmn;

import ch.ethz.biol.cell.imageprocessing.objmask.provider.ObjMaskProviderSeededObjSgmn;
import ch.ethz.biol.cell.imageprocessing.objmask.provider.ObjMaskProviderSgmn;
import ch.ethz.biol.cell.sgmn.objmask.ObjMaskSgmnMinimaImposition;
import ch.ethz.biol.cell.sgmn.objmask.watershed.minimaimposition.MinimaImpositionGrayscaleReconstruction;
import ch.ethz.biol.cell.sgmn.objmask.watershed.minimaimposition.grayscalereconstruction.GrayscaleReconstructionRobinson;
import ch.ethz.biol.cell.sgmn.objmask.watershed.yeong.ObjMaskSgmnWatershedYeong;

/**
 * Beans related to segmentation
 * 
 * @author FEEHANO
 *
 */
class FactorySgmn {
	
	public static ObjMaskProvider minimaUnmerged( BinaryImgChnlProvider mask, ChnlProvider distanceTransform ) {
		ObjMaskProviderSgmn provider = new ObjMaskProviderSgmn();
		provider.setBinaryImgChnlProviderMask(mask);
		provider.setChnlProvider(distanceTransform);
		provider.setSgmn( watershedSgmnForMinima() );
		return provider;
	}
	
	public static ObjMaskProvider watershedSegment( ObjMaskProvider sourceObjs, ObjMaskProvider seeds, ChnlProvider distanceTransform ) {
		ObjMaskProviderSeededObjSgmn provider = new ObjMaskProviderSeededObjSgmn();
		provider.setObjMaskProviderSourceObjs(sourceObjs);
		provider.setObjMaskProviderSeeds(seeds);
		provider.setChnlProvider(distanceTransform);
		provider.setSgmn( watershedSgmnForSegments() );
		return provider;
	}
		
	private static ObjMaskSgmn watershedSgmnForMinima() {
		ObjMaskSgmnWatershedYeong sgmn = new ObjMaskSgmnWatershedYeong();
		sgmn.setExitWithMinima(true);
		return sgmn;
	}
	
	private static ObjMaskSgmn watershedSgmnForSegments() {
		
		ObjMaskSgmnMinimaImposition impose = new ObjMaskSgmnMinimaImposition();
		impose.setSgmn( new ObjMaskSgmnWatershedYeong() );
		impose.setMinimaImposition( minimaImposion() );
		return impose;

	}
	
	private static MinimaImpositionGrayscaleReconstruction minimaImposion() {
		MinimaImpositionGrayscaleReconstruction impose = new MinimaImpositionGrayscaleReconstruction();
		impose.setGrayscaleReconstruction( new GrayscaleReconstructionRobinson() );
		return impose;
	}
}
