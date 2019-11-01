package ch.ethz.biol.cell.imageprocessing.threshold.calculatelevel;

import org.anchoranalysis.image.bean.threshold.calculatelevel.CalculateLevel;

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

import org.anchoranalysis.image.histogram.Histogram;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class OtsuWang extends CalculateLevel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// TODO LICENSE
	// Taken from Auto_Threshold ImageJ plugin
	@Override
	public int calculateLevel( Histogram histogram ) {
		
		
		//System.out.println("Calculating for histogram:\n");
		//System.out.println( histogram.toString() );
		
		// Otsu's threshold algorithm
		// C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
		// ported to ImageJ plugin by G.Landini
		int k,kStar;  // k = the current threshold; kStar = optimal threshold
		int N;    // N1 = # points with intensity <=k; N = total number of points
		double BCV, BCVmax; // The current Between Class Variance and maximum BCV
		int S, L=histogram.size(); // The total intensity of the image

		// Initialize values:
		S = N = 0;
		for (k=0; k<L; k++){
			S += k * histogram.getCount(k);	// Total histogram intensity
			N += histogram.getCount(k);		// Total number of data points
		}

		BCV = 0;
		BCVmax=0;
		kStar = 0;

		double pSum = 0;
		double pSumMult = 0.0;
		
		long numVoxels = histogram.getTotalCount();
		
		double w0, w1;
		
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
			
			
			double part1 = w0 * Math.pow( variance0 - varT, 2.0 );
			double part2 = w1 * Math.pow( variance1 - varT, 2.0 );
			
			
			//double part1 = w0 * Math.pow( mean0 - meanT, 2.0 );
			//double part2 = w1 * Math.pow( mean1 - meanT, 2.0 );
			
			double weight = 1.0 - ( ((double) histogram.getCount(k))/(2*N));
			//double weight = 1.0;
			
			BCV = (part1 + part2) * weight;
			
			// Calc variance of first class
			
			//System.out.printf( "%d\tw0=%f,%f\tvarT=%f\tmeanT=%f\tmean0=%f,mean1=%f\tvar0=%f,var1=%f\tpart1=%f\tpart2=%f\tweight=%f\tBCV=%f\n",
			//	k, w0, w1, varT, meanT, mean0, mean1, variance0, variance1, part1, part2, weight, BCV );
			
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
				
		//System.out.printf( "\n" );
		//System.out.printf("Otsu level is %d\n", kStar );
		
		// kStar += 1;	// Use QTI convention that intensity -> 1 if intensity >= k
		// (the algorithm was developed for I-> 1 if I <= k.)
		return kStar;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof OtsuWang){
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
