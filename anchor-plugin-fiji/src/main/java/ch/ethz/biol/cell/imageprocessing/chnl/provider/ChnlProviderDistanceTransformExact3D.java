package ch.ethz.biol.cell.imageprocessing.chnl.provider;

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


import java.nio.ByteBuffer;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.binary.BinaryChnl;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBox;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactory;
import org.anchoranalysis.image.extent.ImageDimensions;
import org.anchoranalysis.image.extent.ImageResolution;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverter;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverterToUnsignedByte;
import org.anchoranalysis.image.stack.region.chnlconverter.ChannelConverterToUnsignedShort;
import org.anchoranalysis.image.stack.region.chnlconverter.ConversionPolicy;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.datatype.VoxelDataType;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedByte;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeFloat;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeUnsignedShort;

// TODO LICENSE LICENSE LICENSE

// Euclidian distance transform from ImageJ
//
// Does not re-use the binary image
//
// Uses the aspect ratio (relative distance between z and xy slices)
public class ChnlProviderDistanceTransformExact3D extends ChnlProviderMask {

	// START PROPERTIES
	@BeanField
	private boolean suppressZ = false;
	
	@BeanField
	private double multiplyBy = 1.0;
	
	@BeanField
	private double multiplyByZRes = 1.0;
	
	@BeanField
	private boolean createShort = false;
	
	@BeanField
	private boolean applyRes = false;
	// END PROPERTIES
	
	// We can also change a binary voxel buffer
	public static VoxelBox<ByteBuffer> createDistanceMapForVoxelBox( BinaryVoxelBox<ByteBuffer> bvb, ImageResolution res, boolean suppressZ, double multiplyBy, double multiplyByZRes, boolean createShort, boolean applyRes ) throws CreateException {
		Channel chnlIn = ChannelFactory
				.instance()
				.get(VoxelDataTypeUnsignedByte.INSTANCE)
				.create( bvb.getVoxelBox(), res );
		BinaryChnl binaryChnlIn = new BinaryChnl(chnlIn, bvb.getBinaryValues());
		
		Channel distanceMap = createDistanceMapForChnl(binaryChnlIn, suppressZ, multiplyBy, multiplyByZRes, createShort, applyRes );
		return distanceMap.getVoxelBox().asByte();
	}
	
	public static Channel createDistanceMapForChnl( BinaryChnl chnl, boolean suppressZ, double multiplyBy, double multiplyByZRes, boolean createShort, boolean applyRes ) throws CreateException {
		if( chnl.getBinaryValues().getOnInt()!=255) {
			throw new CreateException("Binary On must be 255");
		}
		
		if( chnl.getBinaryValues().getOffInt()!=0) {
			throw new CreateException("Binary Off must be 0");
		}
		
		
		if (chnl.getDimensions().getExtnt().getZ() > 1 && !suppressZ ) {
			
			double zRelRes = chnl.getDimensions().getRes().getZRelRes(); 
			if (Double.isNaN(zRelRes)) {
				throw new CreateException("Z-resolution is NaN");
			}
			
			if (zRelRes==0) {
				throw new CreateException("Z-resolution is 0");
			}
		}
	
		if (suppressZ) {
		
			Channel chnlOut = createEmptyChnl( createShort, chnl.getDimensions() );
			
			for( int z=0; z<chnl.getDimensions().getExtnt().getZ(); z++ ) {
				BinaryChnl chnlSlice = chnl.extractSlice(z) ;
				Channel distSlice = createDistanceMapForChnlFromPlugin(chnlSlice, true,multiplyBy, multiplyByZRes, createShort, applyRes );
				chnlOut.getVoxelBox().transferPixelsForPlane( z, distSlice.getVoxelBox(), 0, true );
			}
			
			return chnlOut;
			
		} else {
			return createDistanceMapForChnlFromPlugin( chnl, false, multiplyBy, multiplyByZRes, createShort, applyRes );
		}
	}
	
	private static Channel createEmptyChnl( boolean createShort, ImageDimensions dims ) {
		VoxelDataType dataType = createShort ? VoxelDataTypeUnsignedShort.INSTANCE : VoxelDataTypeUnsignedByte.INSTANCE;
		return ChannelFactory.instance().createEmptyUninitialised( dims, dataType );
	}
	
	@Override
	protected Channel createFromMask(BinaryChnl mask) throws CreateException {
		return createDistanceMapForChnl(mask,suppressZ,multiplyBy,multiplyByZRes,createShort,applyRes);
	}

	private static Channel createDistanceMapForChnlFromPlugin( BinaryChnl chnl, boolean suppressZ, double multFactor, double multFactorZ, boolean createShort, boolean applyRes ) throws CreateException {
		
		EDT edtPlugin = new EDT();
		
		// Assumes X and Y have the same resolution
		
		Channel distAsFloat = edtPlugin.compute(
			chnl,
			ChannelFactory.instance().get(VoxelDataTypeFloat.INSTANCE),
			suppressZ,
			multFactorZ
		);
		
		if (applyRes) {
			distAsFloat.getVoxelBox().any().multiplyBy(multFactor * chnl.getDimensions().getRes().getX() );
		} else {
			distAsFloat.getVoxelBox().any().multiplyBy(multFactor);
		}

		ChannelConverter<?> converter = createShort ? new ChannelConverterToUnsignedShort() : new ChannelConverterToUnsignedByte();
		return converter.convert(distAsFloat,ConversionPolicy.CHANGE_EXISTING_CHANNEL);
	}

	public boolean isSuppressZ() {
		return suppressZ;
	}

	public void setSuppressZ(boolean suppressZ) {
		this.suppressZ = suppressZ;
	}

	public double getMultiplyBy() {
		return multiplyBy;
	}

	public void setMultiplyBy(double multiplyBy) {
		this.multiplyBy = multiplyBy;
	}

	public double getMultiplyByZRes() {
		return multiplyByZRes;
	}

	public void setMultiplyByZRes(double multiplyByZRes) {
		this.multiplyByZRes = multiplyByZRes;
	}

	public boolean isCreateShort() {
		return createShort;
	}

	public void setCreateShort(boolean createShort) {
		this.createShort = createShort;
	}

	public boolean isApplyRes() {
		return applyRes;
	}

	public void setApplyRes(boolean applyRes) {
		this.applyRes = applyRes;
	}
}
