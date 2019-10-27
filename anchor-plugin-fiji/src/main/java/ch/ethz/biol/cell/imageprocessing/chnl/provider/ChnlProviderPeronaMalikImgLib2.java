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


import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion.DiffusionFunction;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.NativeImg;
import net.imglib2.type.numeric.RealType;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.bean.annotation.Positive;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.convert.ImgLib2Wrap;
import org.anchoranalysis.image.extent.Extent;

public class ChnlProviderPeronaMalikImgLib2 extends ChnlProvider {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// START BEAN PROPERTIES
	@BeanField
	private ChnlProvider chnlProvider;
	
	@BeanField @Positive
	private double kappa;
	
	@BeanField @Positive
	private double deltat;
	
	@BeanField
	private boolean do3D;
	
	@BeanField
	private int iterations = 30;
	
	@BeanField
	private boolean strongEdgeEnhancer = true;	// Enables the StrongEdgeEnhancer diffusion function
	// END BEAN PROPERTIES
	
	private static <T extends RealType<T>,S> void doDiffusion( NativeImg<T,S> img, double deltat, DiffusionFunction df, int iterations ) throws IncompatibleTypeException {
		for( int i=0; i<iterations; i++) {
			PeronaMalikAnisotropicDiffusion.inFloatInPlace(img, deltat, df );
		}
	}
	
	// Assumes XY res are identical
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Chnl diffusion( Chnl chnl, double deltat, DiffusionFunction df, int iterations, boolean do3D ) throws CreateException {
		
		Extent e = chnl.getDimensions().getExtnt();
		try {
			if (do3D) {
				NativeImg img = ImgLib2Wrap.wrap( chnl.getVoxelBox(), true );
				doDiffusion(img,deltat,df,iterations);
			} else {
			
				for( int z=0; z<chnl.getDimensions().getZ(); z++) {
					NativeImg img = ImgLib2Wrap.wrap( chnl.getVoxelBox().any().getPixelsForPlane(z), e );
					doDiffusion(img,deltat,df,iterations);
				}
			}
			
			return chnl;
		} catch (IncompatibleTypeException e1) {
			throw new CreateException(e1);
		}
	}
	
	private DiffusionFunction createDiffusionFunction() {
		if (strongEdgeEnhancer) {
			return new PeronaMalikAnisotropicDiffusion.StrongEdgeEnhancer(kappa);
		} else {
			return new PeronaMalikAnisotropicDiffusion.WideRegionEnhancer(kappa);
		} 
	}
	
	@Override
	public Chnl create() throws CreateException {
		DiffusionFunction df = createDiffusionFunction();
		return diffusion( chnlProvider.create(), deltat, df, iterations, do3D );

	}

	public ChnlProvider getChnlProvider() {
		return chnlProvider;
	}

	public void setChnlProvider(ChnlProvider chnlProvider) {
		this.chnlProvider = chnlProvider;
	}


	public double getKappa() {
		return kappa;
	}


	public void setKappa(double kappa) {
		this.kappa = kappa;
	}


	public double getDeltat() {
		return deltat;
	}


	public void setDeltat(double deltat) {
		this.deltat = deltat;
	}


	public boolean isDo3D() {
		return do3D;
	}


	public void setDo3D(boolean do3d) {
		do3D = do3d;
	}


	public int getIterations() {
		return iterations;
	}


	public void setIterations(int iterations) {
		this.iterations = iterations;
	}


	public boolean isStrongEdgeEnhancer() {
		return strongEdgeEnhancer;
	}


	public void setStrongEdgeEnhancer(boolean strongEdgeEnhancer) {
		this.strongEdgeEnhancer = strongEdgeEnhancer;
	}




}
