/* (C)2020 */
package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.FloatBuffer;
import lombok.Getter;
import org.anchoranalysis.image.voxel.box.VoxelBox;

class EDTDimensionY extends EDTOneDimension {

    @Getter private float multiplyConstant;

    public EDTDimensionY(VoxelBox<FloatBuffer> out, float multiplyConstant) {
        super(out, false);
        this.multiplyConstant = multiplyConstant;
    }

    public final void set(int x, float value) {
        putIntoPuffer(x, value);
    }
}
