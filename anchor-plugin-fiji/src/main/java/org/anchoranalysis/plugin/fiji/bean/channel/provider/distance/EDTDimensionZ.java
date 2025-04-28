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
import org.anchoranalysis.image.voxel.buffer.primitive.UnsignedByteBuffer;

/** Computes the Euclidean Distance Transform along the Z dimension. */
class EDTDimensionZ extends EDTDimensionBase {
    private byte[][] inSlice;
    private float[][] outSlice;
    private int offset;
    private int bufferXYSize;

    /** The constant to multiply the distance values by. */
    @Getter private float multiplyConstant;

    /**
     * Creates a new instance for computing EDT along the Z dimension.
     *
     * @param in the input {@link Voxels} containing unsigned byte data
     * @param out the output {@link Voxels} to store float results
     * @param multiplyConstant the constant to multiply the distance values by
     */
    public EDTDimensionZ(
            Voxels<UnsignedByteBuffer> in, Voxels<FloatBuffer> out, float multiplyConstant) {
        super(in.extent().z());

        this.multiplyConstant = multiplyConstant;

        int sizeZ = in.extent().z();

        bufferXYSize = in.extent().areaXY();

        inSlice = new byte[sizeZ][];
        outSlice = new float[sizeZ][];
        for (int i = 0; i < sizeZ; i++) {
            inSlice[i] = in.sliceBuffer(i).array();
            outSlice[i] = out.sliceBuffer(i).array();
        }
        offset = -1;
    }

    @Override
    public final float get(int x) {
        return inSlice[x][offset] == 0 ? 0 : Float.MAX_VALUE;
    }

    @Override
    public final void set(int x, float value) {
        outSlice[x][offset] = value;
    }

    /**
     * Moves to the next row (XY plane) for processing.
     *
     * @return true if there is a next row to process, false otherwise
     */
    public final boolean nextRow() {
        return ++offset < bufferXYSize;
    }
}
