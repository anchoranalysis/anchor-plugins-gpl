/* (C)2020 */
package ch.ethz.biol.cell.imageprocessing.threshold;

/*
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2019 F. Hoffmann-La Roche Ltd
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import fiji.threshold.Auto_Threshold;
import ij.ImagePlus;
import java.nio.ByteBuffer;
import java.util.Optional;
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
    @BeanField private String method = "";

    @BeanField private boolean noBlack = false;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public boolean isNoBlack() {
        return noBlack;
    }

    public void setNoBlack(boolean noBlack) {
        this.noBlack = noBlack;
    }
}
