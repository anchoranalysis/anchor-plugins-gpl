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


import ij.ImagePlus;
import process3d.MinMaxMedian;

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.bean.provider.ChnlProvider;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.convert.IJWrap;

public class ChnlProviderMinFilterIJ3D extends ChnlProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// START BEAN PROPERTIES
	@BeanField
	private ChnlProvider chnlProvider;
	// END BEAN PROPERTIES
	
	private Chnl median3d( Chnl chnl ) throws CreateException {
		
		ImagePlus imp = IJWrap.createImagePlus(chnl);
		
		//IntImage3D ii3d = new IntImage3D( imp.getStack() );
		//ii3d.medianFilter(radius, radius, radius);
		
		imp = MinMaxMedian.convolve(imp, MinMaxMedian.MINIMUM);
		
		return IJWrap.chnlFromImagePlus(imp, chnl.getDimensions().getRes() );
	}
	
	@Override
	public Chnl create() throws CreateException {
		return median3d(chnlProvider.create());
	}

	
	public ChnlProvider getChnlProvider() {
		return chnlProvider;
	}

	public void setChnlProvider(ChnlProvider chnlProvider) {
		this.chnlProvider = chnlProvider;
	}




}
