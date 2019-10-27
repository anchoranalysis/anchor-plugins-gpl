package ch.ethz.biol.cell.imageprocessing.chnl.provider;

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


import java.nio.ByteBuffer;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.BinaryImgChnlProvider;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.binary.BinaryChnl;
import org.anchoranalysis.image.binary.voxel.BinaryVoxelBox;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.chnl.factory.ChnlFactory;
import org.anchoranalysis.image.extent.ImageDim;
import org.anchoranalysis.image.extent.ImageRes;
import org.anchoranalysis.image.stack.region.chnlconverter.ChnlConverter;
import org.anchoranalysis.image.stack.region.chnlconverter.ChnlConverterToByte;
import org.anchoranalysis.image.stack.region.chnlconverter.ChnlConverterToShort;
import org.anchoranalysis.image.stack.region.chnlconverter.ConversionPolicy;
import org.anchoranalysis.image.voxel.box.VoxelBox;
import org.anchoranalysis.image.voxel.datatype.VoxelDataType;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeByte;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeFloat;
import org.anchoranalysis.image.voxel.datatype.VoxelDataTypeShort;

// TODO LICENSE LICENSE LICENSE

// Euclidian distance transform from ImageJ
//
// Does not re-use the binary image
//
// Uses the aspect ratio (relative distance between z and xy slices)
public class ChnlProviderDistanceTransformExact3D extends ChnlProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// START PROPERTIES
	@BeanField
	private BinaryImgChnlProvider binaryImgChnlProvider;
	
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
	public static VoxelBox<ByteBuffer> createDistanceMapForVoxelBox( BinaryVoxelBox<ByteBuffer> bvb, ImageRes res, boolean suppressZ, double multiplyBy, double multiplyByZRes, boolean createShort, boolean applyRes ) throws CreateException {
		Chnl chnlIn = ChnlFactory
				.instance()
				.get(VoxelDataTypeByte.instance)
				.create( bvb.getVoxelBox(), res );
		BinaryChnl binaryChnlIn = new BinaryChnl(chnlIn, bvb.getBinaryValues());
		
		Chnl distanceMap = createDistanceMapForChnl(binaryChnlIn, suppressZ, multiplyBy, multiplyByZRes, createShort, applyRes );
		return distanceMap.getVoxelBox().asByte();
	}
	
	public static Chnl createDistanceMapForChnl( BinaryChnl chnl, boolean suppressZ, double multiplyBy, double multiplyByZRes, boolean createShort, boolean applyRes ) throws CreateException {
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
		
			Chnl chnlOut = createEmptyChnl( createShort, chnl.getDimensions() );
			
			for( int z=0; z<chnl.getDimensions().getExtnt().getZ(); z++ ) {
				BinaryChnl chnlSlice = chnl.extractSlice(z) ;
				Chnl distSlice = createDistanceMapForChnlFromPlugin(chnlSlice, true,multiplyBy, multiplyByZRes, createShort, applyRes );
				chnlOut.getVoxelBox().transferPixelsForPlane( z, distSlice.getVoxelBox(), 0, true );
			}
			
			return chnlOut;
			
		} else {
			return createDistanceMapForChnlFromPlugin( chnl, false, multiplyBy, multiplyByZRes, createShort, applyRes );
		}
	}
	
	private static Chnl createEmptyChnl( boolean createShort, ImageDim dims ) {
		VoxelDataType dataType = createShort ? VoxelDataTypeShort.instance : VoxelDataTypeByte.instance;
		return ChnlFactory.instance().createEmptyUninitialised( dims, dataType );
	}
	
	@Override
	public Chnl create() throws CreateException {
		
		BinaryChnl chnl = binaryImgChnlProvider.create();
		return createDistanceMapForChnl(chnl,suppressZ,multiplyBy,multiplyByZRes,createShort,applyRes);
	}
	

	private static Chnl createDistanceMapForChnlFromPlugin( BinaryChnl chnl, boolean suppressZ, double multFactor, double multFactorZ, boolean createShort, boolean applyRes ) throws CreateException {
		
		EDT edtPlugin = new EDT();
		
		// Assumes X and Y have the same resolution
		
		Chnl distAsFloat = edtPlugin.compute(
			chnl,
			ChnlFactory.instance().get(VoxelDataTypeFloat.instance),
			suppressZ,
			multFactorZ
		);
		
		if (applyRes) {
			distAsFloat.getVoxelBox().any().multiplyBy(multFactor * chnl.getDimensions().getRes().getX() );
		} else {
			distAsFloat.getVoxelBox().any().multiplyBy(multFactor);
		}

		ChnlConverter<?> converter = createShort ? new ChnlConverterToShort() : new ChnlConverterToByte();
		return converter.convert(distAsFloat,ConversionPolicy.CHANGE_EXISTING_CHANNEL);
	}

	public BinaryImgChnlProvider getBinaryImgChnlProvider() {
		return binaryImgChnlProvider;
	}

	public void setBinaryImgChnlProvider(BinaryImgChnlProvider binaryImgChnlProvider) {
		this.binaryImgChnlProvider = binaryImgChnlProvider;
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
