package ch.ethz.biol.cell.imageprocessing.binaryimgchnl.provider;

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
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;

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
			VoxelDataTypeUnsignedByte.instance
		);
		
		VoxelBox<ByteBuffer> vb = chnlOut.getVoxelBox().asByte();
		
		Auto_Local_Threshold at = new Auto_Local_Threshold();
		
		
		for (int z=0; z<chnl.getDimensions().getZ(); z++) {
			ImagePlus ip = IJWrap.createImagePlus(stack.extractSlice(z), false);	
			
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
