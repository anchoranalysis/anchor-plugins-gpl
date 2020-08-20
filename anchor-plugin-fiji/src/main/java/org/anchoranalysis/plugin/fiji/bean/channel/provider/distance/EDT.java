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

package org.anchoranalysis.plugin.fiji.bean.channel.provider.distance;

import java.nio.FloatBuffer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.anchoranalysis.image.binary.mask.Mask;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactorySingleType;
import org.anchoranalysis.image.voxel.Voxels;

/**
 * An Euclidian Distance transform derived from Fiji_Plugins.jar in Imagej
 *
 * <p>The idea of the Euclidean Distance Transform is to get the distance of every outside pixel to
 * the nearest outside pixel.
 *
 * <p>We use the algorithm proposed in
 *
 * <pre>
 * @TECHREPORT{Felzenszwalb04distancetransforms,
 * author = {Pedro F. Felzenszwalb and Daniel P. Huttenlocher},
 * title = {Distance transforms of sampled functions},
 * institution = {Cornell Computing and Information Science},
 * year = {2004}
 * }
 * </pre>
 *
 * Felzenszwalb & Huttenlocher's idea is to extend the concept to a broader one, namely to minimize
 * not only the distance to an outside pixel, but to minimize the distance plus a value that depends
 * on the outside pixel.
 *
 * <p>In mathematical terms: we determine the minimum of the term
 *
 * <p><code>g(x) = min(d^2(x, y) + f(y) for all y)</code>
 *
 * <p>where y runs through all pixels and d^2 is the square of the Euclidean distance. For the
 * Euclidean distance transform, f(y) is 0 for all outside pixels, and infinity for all inside
 * pixels, and the result is the square root of g(x).
 *
 * <p>The trick is to calculate g in one dimension, store the result, and use it as f(y) in the next
 * dimension. Continue until you covered all dimensions.
 *
 * <p>In order to find the minimum in one dimension (i.e. row by row), the following fact is
 * exploited: for two different <code>y1 < y2, (x - y1)^2 + f(y1) < (x - y2)^2 + f(y2)</code> for
 * <code>x < s</code>, where s is the intersection point of the two parabolae (there is the corner
 * case where one parabola is always lower than the other one, in that case there is no
 * intersection).
 *
 * <p>Using this fact, for each row of n elements, a maximum number of n parabolae are constructed,
 * adding them one by one for each y, adjusting the range of x for which this y yields the minimum,
 * possibly overriding a number of previously added parabolae.
 *
 * <p>At most n parabolae can be added, so the complexity is still linear.
 *
 * <p>After this step, the list of parabolae is iterated to calculate the values for g(x).
 *
 * <p>The license in FIJI indicates this particular plugin is GPL/PD (Public Domain).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class EDT {
    public static Channel compute(
            Mask mask,
            ChannelFactorySingleType factory,
            boolean suppressZ,
            double multiplyAspectRatio) {

        Channel result = factory.createEmptyInitialised(mask.dimensions());

        Voxels<FloatBuffer> voxelsResult = result.voxels().asFloat();

        float zMult = suppressZ ? 1.0f : (float) Math.pow(multiplyAspectRatio, 2);
        new EDTDimensionZ(mask.voxels(), voxelsResult, zMult).compute();
        new EDTDimensionY(voxelsResult, 1.0f).compute();
        new EDTDimensionX(voxelsResult, 1.0f).compute();
        return result;
    }
}
