package ch.ethz.biol.cell.imageprocessing.binaryimgchnl.provider;

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
import ij.process.ImageProcessor;

import java.nio.ByteBuffer;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.BinaryImgChnlProvider;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.binary.BinaryChnl;
import org.anchoranalysis.image.binary.values.BinaryValues;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.chnl.factory.ChnlFactory;
import org.anchoranalysis.image.convert.IJWrap;
import org.anchoranalysis.image.extent.ImageDim;
import org.anchoranalysis.image.stack.Stack;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.buffer.VoxelBufferByte;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeByte;

import fiji.threshold.Auto_Local_Threshold;

public class BinaryImgChnlProviderAutoLocalThrshld extends BinaryImgChnlProvider {
	
	// START BEAN
	@BeanField
	private ChnlProvider chnlProvider;
	
	// "Bernsen", "Mean", "Median", "MidGrey", "Niblack", "Sauvola" 
	@BeanField
	private String method="";
	
	@BeanField
	private int radius = 15;
	// END BEAN
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public BinaryChnl create() throws CreateException {
		Chnl chnl = chnlProvider.create().duplicate();
		
		Stack stack = new Stack( chnl );
		
		Chnl chnlOut = ChnlFactory.instance().createEmptyUninitialised(
			new ImageDim(chnl.getDimensions()),
			VoxelDataTypeByte.instance
		);
		
		VoxelBox<ByteBuffer> vb = chnlOut.getVoxelBox().asByte();
		
		Auto_Local_Threshold at = new Auto_Local_Threshold();
		
		
		for (int z=0; z<chnl.getDimensions().getZ(); z++) {
			ImagePlus ip = IJWrap.createImagePlus(stack.extractSlice(z), 1, false);	
			
			Object[] ret = at.exec(ip, method, radius, 0, 0, true);
			ImagePlus ipOut = (ImagePlus) ret[0];
			
			ImageProcessor processor = ipOut.getImageStack().getProcessor(1);
			byte[] arr = (byte[]) processor.getPixels();
			vb.setPixelsForPlane(z, VoxelBufferByte.wrap(arr));
		}
	
		return new BinaryChnl(chnlOut, BinaryValues.getDefault());
		
	}

	public ChnlProvider getChnlProvider() {
		return chnlProvider;
	}

	public void setChnlProvider(ChnlProvider chnlProvider) {
		this.chnlProvider = chnlProvider;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

}
