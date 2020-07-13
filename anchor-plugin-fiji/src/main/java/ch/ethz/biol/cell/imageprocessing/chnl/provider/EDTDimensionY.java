package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.FloatBuffer;

import org.anchoranalysis.image.voxel.box.VoxelBox;

import lombok.Getter;

class EDTDimensionY extends EDTOneDimension {
	
	@Getter
	private float multiplyConstant;
	
	public EDTDimensionY(VoxelBox<FloatBuffer> out, float multiplyConstant) {
		super(out, false);
		this.multiplyConstant = multiplyConstant;
	}

	public final void set(int x, float value) {
		putIntoPuffer( x, value );
	}
}