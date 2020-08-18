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
import org.anchoranalysis.image.binary.voxel.BinaryVoxels;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelsFactory;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.histogram.Histogram;
import org.anchoranalysis.image.object.ObjectMask;
import org.anchoranalysis.image.voxel.VoxelsWrapper;
import org.anchoranalysis.image.voxel.datatype.UnsignedByte;

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
    public BinaryVoxels<ByteBuffer> threshold(
            VoxelsWrapper inputBuffer,
            BinaryValuesByte bvOut,
            Optional<Histogram> histogram,
            Optional<ObjectMask> objectMask)
            throws OperationFailedException {

        if (objectMask.isPresent()) {
            throw new OperationFailedException("A mask is not supported for this operation");
        }

        ImagePlus ip = IJWrap.createImagePlus(inputBuffer);

        Auto_Threshold at = new Auto_Threshold();

        at.exec(ip, method, false, noBlack, true, false, false, true);

        VoxelsWrapper voxelsOut = IJWrap.voxelsFromImagePlus(ip);

        assert (voxelsOut.getVoxelDataType().equals(UnsignedByte.INSTANCE));

        return BinaryVoxelsFactory.reuseByte(voxelsOut.asByte(), bvOut.createInt());
    }
}
