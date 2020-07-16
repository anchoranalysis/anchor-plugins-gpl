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
import org.anchoranalysis.image.binary.mask.Mask;
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

import lombok.Getter;
import lombok.Setter;


/**
 * Euclidian Distance Transform from ImageJ that works on 2D as well as 3D z-stacks.
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/Distance_transform">Distance transform on Wikipedia</a>.
 * <p>
 * A new channel is always created i.e. the input channel is unchanged.
 * <p>
 * The plugin uses aspect ratio (relative distance between z and xy slices) in its distance calculations.
 *  
 * @author Owen Feehan
 *
 */
public class ChnlProviderDistanceTransformExact3D extends ChnlProviderMask {

	// START PROPERTIES
	@BeanField @Getter @Setter
	private boolean suppressZ = false;
	
	@BeanField @Getter @Setter
	private double multiplyBy = 1.0;
	
	@BeanField @Getter @Setter
	private double multiplyByZRes = 1.0;
	
	@BeanField @Getter @Setter
	private boolean createShort = false;
	
	@BeanField @Getter @Setter
	private boolean applyRes = false;
	// END PROPERTIES
	
	// We can also change a binary voxel buffer
	public static VoxelBox<ByteBuffer> createDistanceMapForVoxelBox( BinaryVoxelBox<ByteBuffer> bvb, ImageResolution res, boolean suppressZ, double multiplyBy, double multiplyByZRes, boolean createShort, boolean applyRes ) throws CreateException {
		Channel chnlIn = ChannelFactory
				.instance()
				.get(VoxelDataTypeUnsignedByte.INSTANCE)
				.create( bvb.getVoxelBox(), res );
		Mask binaryChnlIn = new Mask(chnlIn, bvb.getBinaryValues());
		
		Channel distanceMap = createDistanceMapForChnl(binaryChnlIn, suppressZ, multiplyBy, multiplyByZRes, createShort, applyRes );
		return distanceMap.getVoxelBox().asByte();
	}
	
	public static Channel createDistanceMapForChnl( Mask chnl, boolean suppressZ, double multiplyBy, double multiplyByZRes, boolean createShort, boolean applyRes ) throws CreateException {
		if( chnl.getBinaryValues().getOnInt()!=255) {
			throw new CreateException("Binary On must be 255");
		}
		
		if( chnl.getBinaryValues().getOffInt()!=0) {
			throw new CreateException("Binary Off must be 0");
		}
		
		
		if (chnl.getDimensions().getExtent().getZ() > 1 && !suppressZ ) {
			
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
			
			for( int z=0; z<chnl.getDimensions().getExtent().getZ(); z++ ) {
				Mask chnlSlice = chnl.extractSlice(z) ;
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
	protected Channel createFromMask(Mask mask) throws CreateException {
		return createDistanceMapForChnl(mask,suppressZ,multiplyBy,multiplyByZRes,createShort,applyRes);
	}

	private static Channel createDistanceMapForChnlFromPlugin( Mask chnl, boolean suppressZ, double multFactor, double multFactorZ, boolean createShort, boolean applyRes ) {
		
		// Assumes X and Y have the same resolution
		
		Channel distAsFloat = EDT.compute(
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
}
