package ch.ethz.biol.cell.imageprocessing.threshold;

import fiji.threshold.Auto_Threshold;
import ij.ImagePlus;
import java.nio.ByteBuffer;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.bean.threshold.Thresholder;
import org.anchoranalysis.image.binary.values.BinaryValuesByte;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBox;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBoxByte;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.histogram.Histogram;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.voxel.box.VoxelBoxWrapper;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;

public class ThresholderAutoIJ extends Thresholder {

    // START BEAN PROPERTIES
    /**
     * One of the following strings to identify ImageJ's thresholding algorithms (or an empty string
     * for the default).
     *
     * <p>Default, Huang, "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError(I)",
     * "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen"
     */
    @BeanField @Getter @Setter private String method = "";

    @BeanField @Getter @Setter private boolean noBlack = false;
    // END BEAN PROPERTIES

    @Override
    public BinaryVoxelBox<ByteBuffer> threshold(
            VoxelBoxWrapper inputBuffer,
            BinaryValuesByte bvOut,
            Optional<Histogram> histogram,
            Optional<ObjectMask> mask)
            throws OperationFailedException {

        if (mask.isPresent()) {
            throw new OperationFailedException("A mask is not supported for this operation");
        }

        ImagePlus ip = IJWrap.createImagePlus(inputBuffer);

        Auto_Threshold at = new Auto_Threshold();

        at.exec(ip, method, false, noBlack, true, false, false, true);

        VoxelBoxWrapper vbOut = IJWrap.voxelBoxFromImagePlus(ip);

        assert (vbOut.getVoxelDataType().equals(VoxelDataTypeUnsignedByte.INSTANCE));

        return new BinaryVoxelBoxByte(vbOut.asByte(), bvOut.createInt());
    }
}
