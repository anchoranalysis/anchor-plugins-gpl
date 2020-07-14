package anchor.fiji.bean.define.adder;

/*-
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2016 - 2019 ETH Zurich, University of Zurich, Owen Feehan
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
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.bean.segment.object.SegmentChannelIntoObjects;
import org.anchoranalysis.plugin.image.bean.object.segment.ImposeMinima;
import org.anchoranalysis.plugin.image.bean.object.segment.watershed.minima.MinimaImpositionGrayscaleReconstruction;
import org.anchoranalysis.plugin.image.bean.object.segment.watershed.minima.grayscalereconstruction.GrayscaleReconstructionRobinson;
import org.anchoranalysis.plugin.image.bean.object.segment.watershed.yeong.WatershedYeong;

import ch.ethz.biol.cell.imageprocessing.objmask.provider.ObjMaskProviderSeededObjSgmn;
import ch.ethz.biol.cell.imageprocessing.objmask.provider.ObjMaskProviderSgmn;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Beans related to segmentation
 * 
 * @author Owen Feehan
 *
 */
@NoArgsConstructor(access=AccessLevel.PRIVATE)
class FactorySgmn {
	
	public static ObjectCollectionProvider minimaUnmerged( BinaryChnlProvider mask, ChnlProvider distanceTransform ) {
		ObjMaskProviderSgmn provider = new ObjMaskProviderSgmn();
		provider.setMask(mask);
		provider.setChnl(distanceTransform);
		provider.setSgmn( watershedSgmnForMinima() );
		return provider;
	}
	
	public static ObjectCollectionProvider watershedSegment( ObjectCollectionProvider sourceObjs, ObjectCollectionProvider seeds, ChnlProvider distanceTransform ) {
		ObjMaskProviderSeededObjSgmn provider = new ObjMaskProviderSeededObjSgmn();
		provider.setObjectsSource(sourceObjs);
		provider.setObjectsSeeds(seeds);
		provider.setChnl(distanceTransform);
		provider.setSgmn( watershedSgmnForSegments() );
		return provider;
	}
		
	private static SegmentChannelIntoObjects watershedSgmnForMinima() {
		WatershedYeong sgmn = new WatershedYeong();
		sgmn.setExitWithMinima(true);
		return sgmn;
	}
	
	private static SegmentChannelIntoObjects watershedSgmnForSegments() {
		
		ImposeMinima impose = new ImposeMinima();
		impose.setSegment( new WatershedYeong() );
		impose.setMinimaImposition( minimaImposion() );
		return impose;

	}
	
	private static MinimaImpositionGrayscaleReconstruction minimaImposion() {
		MinimaImpositionGrayscaleReconstruction impose = new MinimaImpositionGrayscaleReconstruction();
		impose.setGrayscaleReconstruction( new GrayscaleReconstructionRobinson() );
		return impose;
	}
}
