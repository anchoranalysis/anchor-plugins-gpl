package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.FloatBuffer;

import org.anchoranalysis.image.voxel.box.VoxelBox;

import lombok.Getter;

class EDTDimensionX extends EDTOneDimension {
	
	@Getter
	private float multiplyConstant;
	
	public EDTDimensionX(VoxelBox<FloatBuffer> out, float multiplyConstant) {
		super(out, true);
		this.multiplyConstant = multiplyConstant;
	}

	public final void set(int x, float value) {
		putIntoPuffer(x, (float)Math.sqrt(value));
	}
}