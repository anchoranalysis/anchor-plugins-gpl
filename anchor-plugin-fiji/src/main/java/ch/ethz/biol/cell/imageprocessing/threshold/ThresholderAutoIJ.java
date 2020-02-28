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


import ij.ImagePlus;

import java.nio.ByteBuffer;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.bean.threshold.Thresholder;
import org.anchoranalysis.image.binary.values.BinaryValuesByte;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBox;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBoxByte;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.histogram.Histogram;
import org.anchoranalysis.image.objmask.ObjMask;
import org.anchoranalysis.image.voxel.box.VoxelBoxWrapper;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;

import fiji.threshold.Auto_Threshold;

public class ThresholderAutoIJ extends Thresholder {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static Log log = LogFactory.getLog(ThresholderAutoIJ.class);
	
	// START BEAN
	// Default, Huang, "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError(I)", 
	//   "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag",
	//   "Triangle", "Yen"
	@BeanField
	private String method="";	

	@BeanField
	private boolean noBlack = false;
	
	@BeanField
	private int lastThreshold;
	// END BEAN
	
	@Override
	public BinaryVoxelBox<ByteBuffer> threshold(VoxelBoxWrapper inputBuffer, BinaryValuesByte bvOut, Histogram histogram) throws OperationFailedException {
		
		ImagePlus ip = IJWrap.createImagePlus(inputBuffer);
		
		Auto_Threshold at = new Auto_Threshold();
		
		Object[] ret = at.exec(ip, method, false, noBlack, true, false, false, true);
		
		lastThreshold = (Integer) ret[0];
			
		VoxelBoxWrapper vbOut = IJWrap.voxelBoxFromImagePlus( ip );
		
		assert(vbOut.getVoxelDataType().equals(VoxelDataTypeUnsignedByte.instance));
		
		return new BinaryVoxelBoxByte( vbOut.asByte(), bvOut.createInt() );
	}

	@Override
	public int getLastThreshold() {
		return lastThreshold;
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

	@Override
	public BinaryVoxelBox<ByteBuffer> threshold(VoxelBoxWrapper inputBuffer, ObjMask objMask, BinaryValuesByte bvOut, Histogram histogram) {
		throw new IllegalAccessError("Method not supported");
	}
}
