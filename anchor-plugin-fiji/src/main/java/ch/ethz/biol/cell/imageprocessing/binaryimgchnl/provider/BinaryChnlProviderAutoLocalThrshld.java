package ch.ethz.biol.cell.imageprocessing.binaryimgchnl.provider;

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.binary.mask.Mask;
import org.anchoranalysis.image.binary.values.BinaryValues;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactory;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.stack.Stack;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.buffer.VoxelBufferByte;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;

public class BinaryChnlProviderAutoLocalThrshld extends BinaryChnlProviderChnlSource {

    // START BEAN PROPERTIES
    // "Bernsen", "Mean", "Median", "MidGrey", "Niblack", "Sauvola"
    @BeanField @Getter @Setter private String method = "";

    @BeanField @Getter @Setter private int radius = 15;
    // END BEAN PROPERTIES

    @Override
    protected Mask createFromSource(Channel chnl) throws CreateException {

        chnl = chnl.duplicate();

        Stack stack = new Stack(chnl);

        Channel chnlOut =
                ChannelFactory.instance()
                        .createEmptyUninitialised(
                                chnl.getDimensions(), VoxelDataTypeUnsignedByte.INSTANCE);

        VoxelBox<ByteBuffer> vb = chnlOut.getVoxelBox().asByte();

        Auto_Local_Threshold at = new Auto_Local_Threshold();

        for (int z = 0; z < chnl.getDimensions().getZ(); z++) {
            ImagePlus ip = IJWrap.createImagePlus(stack.extractSlice(z), false);

            Object[] ret = at.exec(ip, method, radius, 0, 0, true);
            ImagePlus ipOut = (ImagePlus) ret[0];

            ImageProcessor processor = ipOut.getImageStack().getProcessor(1);
            byte[] arr = (byte[]) processor.getPixels();
            vb.setPixelsForPlane(z, VoxelBufferByte.wrap(arr));
        }

        return new Mask(chnlOut, BinaryValues.getDefault());
    }
}
