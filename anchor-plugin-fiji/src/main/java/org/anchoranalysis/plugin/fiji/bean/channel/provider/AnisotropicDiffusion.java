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
import org.anchoranalysis.bean.xml.exception.ProvisionFailedException;
import org.anchoranalysis.core.functional.FunctionalIterate;
import org.anchoranalysis.image.bean.provider.ChannelProviderUnary;
import org.anchoranalysis.image.core.channel.Channel;
import org.anchoranalysis.image.voxel.convert.imglib2.ConvertToImg;

/**
 * Performs <a href="https://en.wikipedia.org/wiki/Anisotropic_diffusion">Perona-Malik Anisotropic Diffusion</a>.
 *
 * <p>See the <a href="http://icy.bioimageanalysis.org/plugin/anisotropicdiffusion-imglib2/">ImgLib2 plugin documentation</a>.
 * 
 * @author Owen Feehan
 */
public class AnisotropicDiffusion extends ChannelProviderUnary {

    // START BEAN PROPERTIES
    /** A parameter <i>kappa</i> associated with the diffusion functions. */
    @BeanField @Positive @Getter @Setter private double kappa;

    /** the integration constant for the numerical integration scheme. Typically less that 1. */
    @BeanField @Positive @Getter @Setter private double deltat;

    /** Whether to perform the diffusion in 3D direction, or rather only in the 2D plane. */
    @BeanField @Getter @Setter private boolean do3D;

    /** How many iterations to perform diffusion. */
    @BeanField @Getter @Setter @Positive private int iterations = 30;

    /** If true uses, the <i>strong-edge-enhancer</i> diffusion function, otherwise the <i>wide-region-enhancer</i>. */
    @BeanField @Getter @Setter private boolean strongEdgeEnhancer = true;
    // END BEAN PROPERTIES

    @Override
    public Channel createFromChannel(Channel channel) throws ProvisionFailedException {
        return diffusion(channel, createDiffusionFunction());
    }

    // Assumes XY res are identical
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Channel diffusion(Channel channel, DiffusionFunction diffusionFunction)
            throws ProvisionFailedException {

        try {
            if (do3D) {
                Img image = ConvertToImg.from(channel.voxels());
                doDiffusion(image, diffusionFunction);
            } else {
                channel.extent()
                        .iterateOverZ(
                                z -> {
                                    Img image = ConvertToImg.fromSlice(channel.voxels(), z);
                                    doDiffusion(image, diffusionFunction);
                                });
            }

            return channel;
        } catch (IncompatibleTypeException e1) {
            throw new ProvisionFailedException(e1);
        }
    }

    private <T extends RealType<T>> void doDiffusion(
            Img<T> image, DiffusionFunction diffusionFunction) {
        FunctionalIterate.repeat(
                iterations,
                () ->
                        PeronaMalikAnisotropicDiffusion.inFloatInPlace(
                                image, deltat, diffusionFunction));
    }

    private DiffusionFunction createDiffusionFunction() {
        if (strongEdgeEnhancer) {
            return new PeronaMalikAnisotropicDiffusion.StrongEdgeEnhancer(kappa);
        } else {
            return new PeronaMalikAnisotropicDiffusion.WideRegionEnhancer(kappa);
        }
    }
}
