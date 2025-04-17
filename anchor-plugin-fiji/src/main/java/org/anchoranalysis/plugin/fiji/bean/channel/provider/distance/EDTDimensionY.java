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
import lombok.Getter;
import org.anchoranalysis.image.voxel.Voxels;

/**
 * Computes the Euclidean Distance Transform along the Y dimension.
 */
class EDTDimensionY extends EDTOneDimension {

    /** The constant to multiply the distance values by. */
    @Getter private float multiplyConstant;

    /**
     * Creates a new instance for computing EDT along the Y dimension.
     *
     * @param out the {@link Voxels} to store the output
     * @param multiplyConstant the constant to multiply the distance values by
     */
    public EDTDimensionY(Voxels<FloatBuffer> out, float multiplyConstant) {
        super(out, false);
        this.multiplyConstant = multiplyConstant;
    }

    @Override
    protected final void set(int x, float value) {
        putIntoBuffer(x, value);
    }
}