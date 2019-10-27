package ch.ethz.biol.cell.imageprocessing.threshold.calculatelevel;

import org.anchoranalysis.image.bean.threshold.calculatelevel.CalculateLevel;

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

import org.anchoranalysis.image.histogram.Histogram;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

// NB UNUSED UNFISHED Can probably be deleted
public class OtsuFitToGlobalVariance extends CalculateLevel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// TODO LICENSE
	// Taken from Auto_Threshold ImageJ plugin
	@SuppressWarnings("unused")
	@Override
	public int calculateLevel( Histogram histogram ) {
		
		double weightVarianceBackground = 0.3;
		
		
		
		System.out.println("Calculating for histogram:\n");
		System.out.println( histogram.toString() );
		
		// First we do a global 
		
		
		
		// Otsu's threshold algorithm
		// C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
		// ported to ImageJ plugin by G.Landini
		int k,kStar;  // k = the current threshold; kStar = optimal threshold
		int N1, N;    // N1 = # points with intensity <=k; N = total number of points
		double BCV, BCVmax; // The current Between Class Variance and maximum BCV
		double num, denom;  // temporary bookeeping
		int Sk;  // The total intensity for all histogram points <=k
		int S, L=histogram.size(); // The total intensity of the image

		// Initialize values:
		S = N = 0;
		for (k=0; k<L; k++){
			S += k * histogram.getCount(k);	// Total histogram intensity
			N += histogram.getCount(k);		// Total number of data points
		}

		Sk = 0;
		N1 = histogram.getCount(0); // The entry for zero intensity
		BCV = 0;
		BCVmax=-100000000;
		kStar = 0;

		double pSum = 0;
		double pSumMult = 0.0;
		
		long numVoxels = histogram.getTotalCount();
		
		double w0, w1;
		
		double meanT = histogram.mean();
		double varT = histogram.variance();
		
		// Look at each possible threshold value,
		// calculate the between-class variance, and decide if it's a max
		for (k=0; k<L-1; k++) { // No need to check endpoints k = 0 or k = L-1
			
			pSum += histogram.getCount(k);
			
			w0 = pSum / numVoxels;
			w1 = 1 - w0;
			
			pSumMult += histogram.getCount(k) * k;
			
			// Calc mean of first class
			double mean0 = pSumMult / pSum;
			double mean1 = (S-pSumMult) / (N-pSum);
			
			if (Double.isNaN(mean0) || Double.isNaN(mean1)) {
				continue;
			}
			
			assert(mean0>=0);
			assert(mean1>=0);
			
			double variance0 = 0.0;
			for( int i=0; i<=k; i++) {
				variance0 += Math.pow( (i-mean0), 2.0 ) * histogram.getCount(i);
			}
			variance0 /= pSum;
			
			
			double variance1 = 0.0;
			for( int i=(k+1); i<L; i++) {
				variance1 += Math.pow( (i-mean1), 2.0 ) * histogram.getCount(i);
			}
			variance1 /= (N-pSum);
			
			
			//double varT2 = 2.0 * varT * weightVarianceBackground;
			//double varT1 = 2.0 * varT * (1-weightVarianceBackground);
			
			//double part1 = w0 * Math.pow( variance0 - varT, 2.0 );
			//double part2 = w1 * Math.pow( variance1 - varT, 2.0 );
			//double part1 = w0 / variance0;
			//double part2 = w1 / variance1;
			
			double sd0 = Math.sqrt(variance0);
			double sd1 = Math.sqrt(variance1);
			
			//double part1 = 0;// Math.pow(mean0-mean1,2.0);
			//double part2 = -1 * Math.pow(variance0, 1.0)*w1  -1 * Math.pow(variance1, 1.0)*w0; //Math.pow(variance0,2.0);
			
			double part1 = 1 * w0 * Math.pow( mean0 - meanT, 2.0 );
			double part2 = w1 * Math.pow( mean1 - meanT, 2.0 );
			
			//double weight = 1.0 - ( ((double) histogram.getVal(k))/(2*N));
			double weight = 1.0;
			
			BCV = (part1 + part2) * weight;
			
			
			
			// Calc variance of first class
			
			System.out.printf( "%d\tw0=%f,%f\tvarT=%f\tmeanT=%f\tmean0=%f,mean1=%f\tvar0=%f,var1=%f\tpart1=%f\tpart2=%f\tweight=%f\tBCV=%f\n",
				k, w0, w1, varT, meanT, mean0, mean1, variance0, variance1, part1, part2, weight, BCV );
			
//			Sk += k * histogram.getVal(k);
//			N1 += histogram.getVal(k);
//
//			// The float casting here is to avoid compiler warning about loss of precision and
//			// will prevent overflow in the case of large saturated images
//			denom = (double)( N1) * (N - N1); // Maximum value of denom is (N^2)/4 =  approx. 3E10
//
//			if (denom != 0 ){
//				// Float here is to avoid loss of precision when dividing
//				num = ( (double)N1 / N ) * S - Sk; 	// Maximum value of num =  255*N = approx 8E7
//				BCV = (num * num) / denom;
//			}
//			else {
//				BCV = 0;
//			}
//			
//			System.out.printf( "%d\tcnt=%d\tBCV=%f\n", k, histogram.getVal(k), BCV );
//
			if (BCV >= BCVmax){ // Assign the best threshold found so far
				BCVmax = BCV;
				kStar = k;
			}
		}
		
		kStar += 1;
				
		System.out.printf( "\n" );
		System.out.printf("Otsu level is %d\n", kStar );
		
		// kStar += 1;	// Use QTI convention that intensity -> 1 if intensity >= k
		// (the algorithm was developed for I-> 1 if I <= k.)
		return kStar;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof OtsuFitToGlobalVariance){
	        return new EqualsBuilder()
	            .isEquals();
	    } else{
	        return false;
	    }
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
			.toHashCode();
	}
}
