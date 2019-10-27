package ch.ethz.biol.cell.imageprocessing.threshold;

/*
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2019 F. Hoffmann-La Roche Ltd
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
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeByte;

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
		
		assert(vbOut.getVoxelDataType().equals(VoxelDataTypeByte.instance));
		
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
