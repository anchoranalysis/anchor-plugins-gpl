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
package org.anchoranalysis.plugin.fiji.bean.threshold;

import fiji.threshold.Auto_Threshold;
import ij.ImagePlus;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.exception.OperationFailedException;
import org.anchoranalysis.image.bean.threshold.Thresholder;
import org.anchoranalysis.image.voxel.VoxelsUntyped;
import org.anchoranalysis.image.voxel.binary.BinaryVoxels;
import org.anchoranalysis.image.voxel.binary.BinaryVoxelsFactory;
import org.anchoranalysis.image.voxel.binary.values.BinaryValuesByte;
import org.anchoranalysis.image.voxel.buffer.primitive.UnsignedByteBuffer;
import org.anchoranalysis.image.voxel.datatype.UnsignedByteVoxelType;
import org.anchoranalysis.image.voxel.object.ObjectMask;
import org.anchoranalysis.io.imagej.convert.ConvertFromImagePlus;
import org.anchoranalysis.io.imagej.convert.ConvertToImagePlus;
import org.anchoranalysis.io.imagej.convert.ImageJConversionException;
import org.anchoranalysis.math.histogram.Histogram;

public class ThresholderAutoIJ extends Thresholder {

    // START BEAN PROPERTIES
    /**
     * One of the following strings to identify ImageJ's thresholding algorithms.
     * 
     * <p>An empty string will use the Default.
     *
     * <p>Default, Huang, "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError(I)",
     * "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen"
     */
    @BeanField @Getter @Setter private String method = "";

    @BeanField @Getter @Setter private boolean noBlack = false;
    // END BEAN PROPERTIES

    @Override
    public BinaryVoxels<UnsignedByteBuffer> threshold(
            VoxelsUntyped inputBuffer,
            BinaryValuesByte binaryValues,
            Optional<Histogram> histogram,
            Optional<ObjectMask> objectMask)
            throws OperationFailedException {

        if (objectMask.isPresent()) {
            throw new OperationFailedException("A mask is not supported for this operation");
        }

        try {
            ImagePlus image = ConvertToImagePlus.from(inputBuffer);
            applyThreshold(image);
            return convertToBinary(image, binaryValues);
        } catch (ImageJConversionException e) {
            throw new OperationFailedException(e);
        }
    }

    private void applyThreshold(ImagePlus image) {
        Auto_Threshold at = new Auto_Threshold();
        at.exec(image, method, false, noBlack, true, false, false, true);
    }

    private static BinaryVoxels<UnsignedByteBuffer> convertToBinary(
            ImagePlus image, BinaryValuesByte binaryValues) throws OperationFailedException {
        VoxelsUntyped thresholdedVoxels = ConvertFromImagePlus.toVoxels(image);

        if (!thresholdedVoxels.getVoxelDataType().equals(UnsignedByteVoxelType.INSTANCE)) {
            throw new OperationFailedException(
                    "The threshold operation returned a data-type that is not unsigned 8-bit");
        }

        return BinaryVoxelsFactory.reuseByte(thresholdedVoxels.asByte(), binaryValues.asInt());
    }
}
