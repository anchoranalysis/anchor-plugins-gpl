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
import org.anchoranalysis.image.extent.Extent;
import org.anchoranalysis.image.voxel.box.VoxelBox;

abstract class EDTOneDimension extends EDTDimensionBase {

    private VoxelBox<FloatBuffer> stack;
    private FloatBuffer slice;
    private int offset;
    private int lastOffset;
    private int rowStride;
    private int columnStride;
    private int sliceIndex;

    public EDTOneDimension(VoxelBox<FloatBuffer> out, boolean iterateX) {
        super(iterateX ? out.extent().getX() : out.extent().getY());
        stack = out;

        Extent e = out.extent();

        columnStride = iterateX ? 1 : e.getX();
        rowStride = iterateX ? e.getX() : 1;
        offset = e.getVolumeXY();
        lastOffset = rowStride * (iterateX ? e.getY() : e.getX());
        sliceIndex = -1;
    }

    public final float get(int x) {
        return slice.get(x * columnStride + offset);
    }

    public final boolean nextRow() {
        offset += rowStride;
        if (offset >= lastOffset) {
            if (++sliceIndex >= stack.extent().getZ()) return false;
            offset = 0;
            slice = stack.getPixelsForPlane(sliceIndex).buffer();
        }
        return true;
    }

    protected void putIntoPuffer(int x, float value) {
        slice.put(x * columnStride + offset, value);
    }
}
