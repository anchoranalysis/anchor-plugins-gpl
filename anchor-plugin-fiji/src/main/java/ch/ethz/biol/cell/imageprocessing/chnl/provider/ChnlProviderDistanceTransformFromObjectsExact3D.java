package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ObjectCollectionProvider;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBox;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactory;
import org.anchoranalysis.image.extent.BoundingBox;
import org.anchoranalysis.image.extent.ImageDimensions;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;

// Euclidian distance transform from ImageJ
//
// Does not re-use the binary image
public class ChnlProviderDistanceTransformFromObjectsExact3D extends ChnlProviderDimSource {

    // START PROPERTIES
    @BeanField @Getter @Setter private ObjectCollectionProvider objects;

    @BeanField @Getter @Setter private boolean suppressZ = false;

    @BeanField @Getter @Setter private boolean createShort = false;
    // END PROPERTIES

    @Override
    protected Channel createFromDim(ImageDimensions dim) throws CreateException {

        Channel chnlOut =
                ChannelFactory.instance()
                        .createEmptyInitialised(dim, VoxelDataTypeUnsignedByte.INSTANCE);
        VoxelBox<ByteBuffer> vbOut = chnlOut.getVoxelBox().asByte();

        for (ObjectMask object : objects.create()) {
            BinaryVoxelBox<ByteBuffer> bvb = object.binaryVoxelBox().duplicate();
            VoxelBox<ByteBuffer> voxelBoxDistance =
                    ChnlProviderDistanceTransformExact3D.createDistanceMapForVoxelBox(
                            bvb,
                            chnlOut.getDimensions().getRes(),
                            suppressZ,
                            1.0,
                            1.0,
                            createShort,
                            false);

            BoundingBox bboxSrc = new BoundingBox(voxelBoxDistance.extent());
            voxelBoxDistance.copyPixelsToCheckMask(
                    bboxSrc,
                    vbOut,
                    object.getBoundingBox(),
                    object.getVoxelBox(),
                    object.getBinaryValuesByte());
        }

        return chnlOut;
    }
}
