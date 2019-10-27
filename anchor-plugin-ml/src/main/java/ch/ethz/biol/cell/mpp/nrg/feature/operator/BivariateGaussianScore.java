package ch.ethz.biol.cell.mpp.nrg.feature.operator;

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

import org.anchoranalysis.bean.annotation.BeanField;
import org.anchoranalysis.feature.bean.Feature;
import org.anchoranalysis.feature.calc.FeatureCalcException;
import org.anchoranalysis.feature.calc.params.FeatureCalcParams;
import org.anchoranalysis.feature.params.FeatureParamsDescriptor;
import org.anchoranalysis.feature.params.ParamTypeUtilities;

import umontreal.ssj.probdistmulti.BiNormalGenzDist;


// LICENSE!!!
// A score between 0 and 1, based upon the CDF of a bivariate gaussian. as one approaches the mean, the score approaches 1.0
public class BivariateGaussianScore extends Feature {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// START BEAN PROPERTIES
	@BeanField
	private Feature item1 = null;
	
	@BeanField
	private Feature item2 = null;
	
	@BeanField
	private Feature itemMean1 = null;
	
	@BeanField
	private Feature itemStdDev1 = null;
	
	@BeanField
	private Feature itemMean2 = null;
	
	@BeanField
	private Feature itemStdDev2 = null;
	// END BEAN PROPERTIES
	
	public static double calc(
		double mean1,
		double stdDev1,
		double mean2,
		double stdDev2,		
		double val1,
		double val2
	) {
		
		// Explicitly, no covariance between each variable
		double cdf = BiNormalGenzDist.cdf(mean1,stdDev1, val1, mean2,stdDev2, val2, 0.0);
		
		if (cdf>0.5) {
			return (1-cdf)*2;
		} else {
			return cdf*2;
		}
	}
	
	@Override
	public double calc( FeatureCalcParams params ) throws FeatureCalcException {
		
		double val1 = getCacheSession().calc( getItem1(), params );
		double val2 = getCacheSession().calc( getItem2(), params );
		
		double mean1 = getCacheSession().calc( getItemMean1(), params );
		double mean2 = getCacheSession().calc( getItemMean2(), params );
		
		double stdDev1 = getCacheSession().calc( getItemStdDev1(), params );
		double stdDev2 = getCacheSession().calc( getItemStdDev2(), params );
		
		// We normalise
		
//		val = val - mean;
//		mean = 0;
//		
//		val = val / stdDev;
//		stdDev = 1;
		
		return calc( mean1, stdDev1, mean2, stdDev2, val1, val2 );
	}

	@Override
	public String getDscrLong() {
		return String.format("pdf(%s,%s,%s,%s,%s,%s)",
			getItem1().getDscrLong(),
			getItem2().getDscrLong(),
			getItemMean1().getDscrLong(),
			getItemStdDev1().getDscrLong(),
			getItemMean2().getDscrLong(),
			getItemStdDev2().getDscrLong()			
		);
	}

	public Feature getItemMean1() {
		return itemMean1;
	}

	public void setItemMean1(Feature itemMean1) {
		this.itemMean1 = itemMean1;
	}

	public Feature getItemStdDev1() {
		return itemStdDev1;
	}

	public void setItemStdDev1(Feature itemStdDev1) {
		this.itemStdDev1 = itemStdDev1;
	}

	public Feature getItemMean2() {
		return itemMean2;
	}

	public void setItemMean2(Feature itemMean2) {
		this.itemMean2 = itemMean2;
	}

	public Feature getItemStdDev2() {
		return itemStdDev2;
	}

	public void setItemStdDev2(Feature itemStdDev2) {
		this.itemStdDev2 = itemStdDev2;
	}

	public Feature getItem1() {
		return item1;
	}

	public void setItem1(Feature item1) {
		this.item1 = item1;
	}

	public Feature getItem2() {
		return item2;
	}

	public void setItem2(Feature item2) {
		this.item2 = item2;
	}

	@Override
	public FeatureParamsDescriptor paramType() throws FeatureCalcException {
		return ParamTypeUtilities.paramTypeForTwo(item1, item2);
	}
}
