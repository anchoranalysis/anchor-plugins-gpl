package ch.ethz.biol.cell.sgmn.objmask.watershed.minimaimposition.grayscalereconstruction;

import ch.ethz.biol.cell.nucleii.landini.GreyscaleReconstruct_;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.box.VoxelBoxWrapper;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;
import org.anchoranalysis.plugin.image.bean.object.segment.watershed.minima.grayscalereconstruction.GrayscaleReconstructionByErosion;

public class GrayscaleReconstruction2DIJ extends GrayscaleReconstructionByErosion {

    @Override
    public VoxelBoxWrapper reconstruction(
            VoxelBoxWrapper mask, VoxelBoxWrapper marker, Optional<ObjectMask> containingMask)
            throws OperationFailedException {

        if (containingMask.isPresent()) {
            throw new OperationFailedException("A mask is not supported for this operation");
        }

        if (!marker.getVoxelDataType().equals(VoxelDataTypeUnsignedByte.INSTANCE)
                || !mask.getVoxelDataType().equals(VoxelDataTypeUnsignedByte.INSTANCE)) {
            throw new OperationFailedException("Only unsigned byte supported for marker image");
        }

        VoxelBox<ByteBuffer> markerCast = marker.asByte();
        VoxelBox<ByteBuffer> maskCast = mask.asByte();

        // We flip everything because the IJ plugin is reconstruction by Dilation, whereas we want
        // reconstruction by Erosion
        markerCast.subtractFrom(VoxelDataTypeUnsignedByte.MAX_VALUE_INT);
        mask.subtractFromMaxValue();

        VoxelBox<ByteBuffer> ret = reconstructionByDilation(maskCast, markerCast);
        ret.subtractFrom(VoxelDataTypeUnsignedByte.MAX_VALUE_INT);

        return new VoxelBoxWrapper(ret);
    }

    private VoxelBox<ByteBuffer> reconstructionByDilation(
            VoxelBox<ByteBuffer> maskVb, VoxelBox<ByteBuffer> markerVb) {

        ImageProcessor processorMask = IJWrap.imageProcessorByte(maskVb.getPlaneAccess(), 0);
        ImageProcessor processorMarker = IJWrap.imageProcessorByte(markerVb.getPlaneAccess(), 0);

        ImagePlus ipMaskImage = new ImagePlus("mask", processorMask);
        ImagePlus ipMarkerImage = new ImagePlus("marker", processorMarker);

        GreyscaleReconstruct_ gr = new GreyscaleReconstruct_();
        Object[] ret = gr.exec(ipMaskImage, ipMarkerImage, "recon", true, false);

        ImagePlus ipRecon = (ImagePlus) ret[1];
        return IJWrap.voxelBoxFromImagePlus(ipRecon).asByte();
    }
}
