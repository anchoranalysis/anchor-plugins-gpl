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

import ij.ImagePlus;
import org.anchoranalysis.bean.xml.exception.ProvisionFailedException;
import org.anchoranalysis.image.bean.provider.ChannelProviderUnary;
import org.anchoranalysis.image.core.channel.Channel;
import org.anchoranalysis.io.imagej.convert.ConvertFromImagePlus;
import org.anchoranalysis.io.imagej.convert.ConvertToImagePlus;
import org.anchoranalysis.io.imagej.convert.ImageJConversionException;
import process3d.MinMaxMedian;

/** Applies a minimum filter to a channel using ImageJ's MinMaxMedian plugin. */
public class MinimumFilter extends ChannelProviderUnary {

    @Override
    public Channel createFromChannel(Channel channel) throws ProvisionFailedException {
        try {
            ImagePlus image = ConvertToImagePlus.from(channel);
            ImagePlus convolved = MinMaxMedian.convolve(image, MinMaxMedian.MINIMUM);
            return ConvertFromImagePlus.toChannel(convolved, channel.resolution());
        } catch (ImageJConversionException e) {
            throw new ProvisionFailedException(e);
        }
    }
}
