package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import ij.ImagePlus;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ChnlProviderOne;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.convert.IJWrap;
import process3d.MinMaxMedian;

public class ChnlProviderMinFilterIJ3D extends ChnlProviderOne {

    @Override
    public Channel createFromChnl(Channel chnl) throws CreateException {
        ImagePlus imp = IJWrap.createImagePlus(chnl);
        imp = MinMaxMedian.convolve(imp, MinMaxMedian.MINIMUM);
        return IJWrap.chnlFromImagePlus(imp, chnl.getDimensions().getRes());
    }
}
