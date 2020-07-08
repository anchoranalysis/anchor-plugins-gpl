package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.FloatBuffer;

import org.anchoranalysis.image.extent.Extent;
import org.anchoranalysis.image.voxel.box.VoxelBox;

abstract class EDTOneDimension extends EDTDimensionBase {
	
	private VoxelBox<FloatBuffer> stack;
	private FloatBuffer slice;
	private int offset, lastOffset, rowStride, columnStride, sliceIndex;

	public EDTOneDimension(VoxelBox<FloatBuffer> out, boolean iterateX) {
		super(iterateX ? out.extent().getX() : out.extent().getY() );
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
			if (++sliceIndex >= stack.extent().getZ())
				return false;
			offset = 0;
			slice = stack.getPixelsForPlane(sliceIndex).buffer();
		}
		return true;
	}

	protected void putIntoPuffer( int x, float value ) {
		slice.put( x * columnStride + offset, value );
	}
}