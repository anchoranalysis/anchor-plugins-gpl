package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.anchoranalysis.image.voxel.box.VoxelBox;

import lombok.Getter;

class EDTDimensionZ extends EDTDimensionBase {
	private byte[][] inSlice;
	private float[][] outSlice;
	private int offset;
	private int bufferXYSize;
	
	@Getter
	private float multiplyConstant;

	public EDTDimensionZ(VoxelBox<ByteBuffer> in, VoxelBox<FloatBuffer> out, float multiplyConstant) {
		super( in.extent().getZ() );
		
		this.multiplyConstant = multiplyConstant;
		
		int d = in.extent().getZ();
		
		bufferXYSize = in.extent().getVolumeXY();
		
		inSlice = new byte[d][];
		outSlice = new float[d][];
		for (int i = 0; i < d; i++) {
			inSlice[i] = (byte[])in.getPixelsForPlane(i).buffer().array();
			outSlice[i] = (float[])out.getPixelsForPlane(i).buffer().array();
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