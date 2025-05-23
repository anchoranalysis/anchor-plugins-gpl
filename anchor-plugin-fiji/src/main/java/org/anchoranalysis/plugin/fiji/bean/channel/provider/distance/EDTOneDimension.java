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
import org.anchoranalysis.image.voxel.Voxels;
import org.anchoranalysis.spatial.box.Extent;

/** Base class for computing Euclidean Distance Transform along a single dimension (X or Y). */
abstract class EDTOneDimension extends EDTDimensionBase {

    private Voxels<FloatBuffer> stack;
    private FloatBuffer slice;
    private int offset;
    private int lastOffset;
    private int rowStride;
    private int columnStride;
    private int sliceIndex;

    /**
     * Creates a new instance for computing EDT along a single dimension.
     *
     * @param out the {@link Voxels} to store the output
     * @param iterateX if true, iterate along X dimension; if false, iterate along Y dimension
     */
    protected EDTOneDimension(Voxels<FloatBuffer> out, boolean iterateX) {
        super(iterateX ? out.extent().x() : out.extent().y());
        stack = out;

        Extent extent = out.extent();

        columnStride = iterateX ? 1 : extent.x();
        rowStride = iterateX ? extent.x() : 1;
        offset = extent.areaXY();
        lastOffset = rowStride * (iterateX ? extent.y() : extent.x());
        sliceIndex = -1;
    }

    @Override
    protected final float get(int x) {
        return slice.get(x * columnStride + offset);
    }

    @Override
    protected final boolean nextRow() {
        offset += rowStride;
        if (offset >= lastOffset) {
            if (++sliceIndex >= stack.extent().z()) return false;
            offset = 0;
            slice = stack.sliceBuffer(sliceIndex);
        }
        return true;
    }

    /**
     * Puts a value into the buffer at a specific position.
     *
     * @param x the x-coordinate
     * @param value the value to put into the buffer
     */
    protected void putIntoBuffer(int x, float value) {
        slice.put(x * columnStride + offset, value);
    }
}
