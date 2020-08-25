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
package org.anchoranalysis.plugin.fiji.bean.channel.provider;

import lombok.Getter;
import lombok.Setter;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion.DiffusionFunction;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.annotation.Positive;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ChannelProviderUnary;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.convert.ImgLib2Wrap;

/**
 * Perona-Malik Anisotropic Diffusion
 *
 * @author Owen Feehan
 */
public class AnisotropicDiffusion extends ChannelProviderUnary {

    // START BEAN PROPERTIES
    @BeanField @Positive @Getter @Setter private double kappa;

    @BeanField @Positive @Getter @Setter private double deltat;

    @BeanField @Getter @Setter private boolean do3D;

    @BeanField @Getter @Setter private int iterations = 30;

    /** Enables the StrongEdgeEnhancer diffusion function */
    @BeanField @Getter @Setter private boolean strongEdgeEnhancer = true;
    // END BEAN PROPERTIES

    // Assumes XY res are identical
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Channel diffusion(
            Channel channel, double deltat, DiffusionFunction df, int iterations, boolean do3D)
            throws CreateException {

        try {
            if (do3D) {
                Img img = ImgLib2Wrap.wrap(channel.voxels());
                doDiffusion(img, deltat, df, iterations);
            } else {
                channel.extent()
                        .iterateOverZ(
                                z -> {
                                    Img img =
                                            ImgLib2Wrap.wrap(
                                                    channel.voxels().slice(z), channel.extent());
                                    doDiffusion(img, deltat, df, iterations);
                                });
            }

            return channel;
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
    public Channel createFromChannel(Channel channel) throws CreateException {
        DiffusionFunction df = createDiffusionFunction();
        return diffusion(channel, deltat, df, iterations, do3D);
    }
}
