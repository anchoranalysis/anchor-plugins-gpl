/* (C)2020 */
package ch.ethz.biol.cell.imageprocessing.chnl.provider;

/*
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2019 F. Hoffmann-La Roche Ltd
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

import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion.DiffusionFunction;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.annotation.Positive;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ChnlProviderOne;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.convert.ImgLib2Wrap;
import org.anchoranalysis.image.extent.Extent;

/**
 * Perona-Malik Anisotropic Diffusion
 *
 * @author Owen Feehan
 */
public class ChnlProviderAnisotropicDiffusion extends ChnlProviderOne {

    // START BEAN PROPERTIES
    @BeanField @Positive private double kappa;

    @BeanField @Positive private double deltat;

    @BeanField private boolean do3D;

    @BeanField private int iterations = 30;

    @BeanField
    private boolean strongEdgeEnhancer = true; // Enables the StrongEdgeEnhancer diffusion function
    // END BEAN PROPERTIES

    // Assumes XY res are identical
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Channel diffusion(
            Channel chnl, double deltat, DiffusionFunction df, int iterations, boolean do3D)
            throws CreateException {

        Extent e = chnl.getDimensions().getExtent();
        try {
            if (do3D) {
                Img img = ImgLib2Wrap.wrap(chnl.getVoxelBox());
                doDiffusion(img, deltat, df, iterations);
            } else {

                for (int z = 0; z < chnl.getDimensions().getZ(); z++) {
                    Img img = ImgLib2Wrap.wrap(chnl.getVoxelBox().any().getPixelsForPlane(z), e);
                    doDiffusion(img, deltat, df, iterations);
                }
            }

            return chnl;
        } catch (IncompatibleTypeException e1) {
            throw new CreateException(e1);
        }
    }

    private static <T extends RealType<T>> void doDiffusion(
            Img<T> img, double deltat, DiffusionFunction df, int iterations) {
        for (int i = 0; i < iterations; i++) {
            PeronaMalikAnisotropicDiffusion.inFloatInPlace(img, deltat, df);
        }
    }

    private DiffusionFunction createDiffusionFunction() {
        if (strongEdgeEnhancer) {
            return new PeronaMalikAnisotropicDiffusion.StrongEdgeEnhancer(kappa);
        } else {
            return new PeronaMalikAnisotropicDiffusion.WideRegionEnhancer(kappa);
        }
    }

    @Override
    public Channel createFromChnl(Channel chnl) throws CreateException {
        DiffusionFunction df = createDiffusionFunction();
        return diffusion(chnl, deltat, df, iterations, do3D);
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public double getDeltat() {
        return deltat;
    }

    public void setDeltat(double deltat) {
        this.deltat = deltat;
    }

    public boolean isDo3D() {
        return do3D;
    }

    public void setDo3D(boolean do3D) {
        this.do3D = do3D;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public boolean isStrongEdgeEnhancer() {
        return strongEdgeEnhancer;
    }

    public void setStrongEdgeEnhancer(boolean strongEdgeEnhancer) {
        this.strongEdgeEnhancer = strongEdgeEnhancer;
    }
}
