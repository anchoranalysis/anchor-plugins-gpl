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

package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.FloatBuffer;
import org.anchoranalysis.image.voxel.Voxels;
import lombok.Getter;

class EDTDimensionY extends EDTOneDimension {

    @Getter private float multiplyConstant;

    public EDTDimensionY(Voxels<FloatBuffer> out, float multiplyConstant) {
        super(out, false);
        this.multiplyConstant = multiplyConstant;
    }

    public final void set(int x, float value) {
        putIntoPuffer(x, value);
    }
}
