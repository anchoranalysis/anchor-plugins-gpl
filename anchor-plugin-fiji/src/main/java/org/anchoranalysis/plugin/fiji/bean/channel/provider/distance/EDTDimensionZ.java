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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import lombok.Getter;
import org.anchoranalysis.image.voxel.Voxels;

class EDTDimensionZ extends EDTDimensionBase {
    private byte[][] inSlice;
    private float[][] outSlice;
    private int offset;
    private int bufferXYSize;

    @Getter private float multiplyConstant;

    public EDTDimensionZ(Voxels<ByteBuffer> in, Voxels<FloatBuffer> out, float multiplyConstant) {
        super(in.extent().z());

        this.multiplyConstant = multiplyConstant;

        int d = in.extent().z();

        bufferXYSize = in.extent().volumeXY();

        inSlice = new byte[d][];
        outSlice = new float[d][];
        for (int i = 0; i < d; i++) {
            inSlice[i] = (byte[]) in.sliceBuffer(i).array();
            outSlice[i] = (float[]) out.sliceBuffer(i).array();
        }
        offset = -1;
    }

    public final float get(int x) {
        return inSlice[x][offset] == 0 ? 0 : Float.MAX_VALUE;
    }

    public final void set(int x, float value) {
        outSlice[x][offset] = value;
    }

    public final boolean nextRow() {
        return ++offset < bufferXYSize;
    }
}
