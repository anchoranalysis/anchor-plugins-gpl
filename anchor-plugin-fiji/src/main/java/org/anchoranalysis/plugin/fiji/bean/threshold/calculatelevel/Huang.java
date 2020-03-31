package org.anchoranalysis.plugin.fiji.bean.threshold.calculatelevel;

import org.anchoranalysis.image.bean.threshold.CalculateLevel;

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

public class Huang extends CalculateLevel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	// http://trac.imagej.net/browser/core/commands/display/src/main/java/imagej/core/commands/display/interactive/threshold/HuangThresholdMethod.java?rev=9704accc7513dd9cf8f840955c7cd6b4b45b7fc7
	@Override
	public int calculateLevel( Histogram histogram ) {
		// Implements Huang's fuzzy thresholding method
		// Uses Shannon's entropy function (one can also use Yager's entropy
		// function) Huang L.-K. and Wang M.-J.J. (1995) "Image Thresholding by
		// Minimizing the Measures of Fuzziness" Pattern Recognition, 28(1): 41-51
		// Reimplemented (to handle 16-bit efficiently) by Johannes Schindelin
		// Jan 31, 2011

		// find first and last non-empty bin
		int first, last;
		for (first = 0; first < histogram.getMaxBin() && histogram.getCount(first) == 0; first++)
		{
			// do nothing
		}
		for (last = histogram.getMaxBin() - 1; last > first && histogram.getCount(last) == 0; last--)
		{
			// do nothing
		}
		if (first == last) return 0;

		// calculate the cumulative density and the weighted cumulative density
		double[] S = new double[last + 1], W = new double[last + 1];
		S[0] = histogram.getCount(0);
		for (int i = Math.max(1, first); i <= last; i++) {
			S[i] = S[i - 1] + histogram.getCount(i);
			W[i] = W[i - 1] + i * histogram.getCount(i);
		}

		// precalculate the summands of the entropy given the absolute difference x
		// - mu (integral)
		double C = (double) last - first;
		double[] Smu = new double[last + 1 - first];
		for (int i = 1; i < Smu.length; i++) {
			double mu = 1 / (1 + Math.abs(i) / C);
			Smu[i] = -mu * Math.log(mu) - (1 - mu) * Math.log(1 - mu);
		}

		// calculate the threshold
		int bestThreshold = 0;
		double bestEntropy = Double.MAX_VALUE;
		for (int threshold = first; threshold <= last; threshold++) {
			double entropy = 0;
			int mu = (int) Math.round(W[threshold] / S[threshold]);
			for (int i = first; i <= threshold; i++)
				entropy += Smu[Math.abs(i - mu)] * histogram.getCount(i);
			mu =
				(int) Math.round((W[last] - W[threshold]) / (S[last] - S[threshold]));
			for (int i = threshold + 1; i <= last; i++)
				entropy += Smu[Math.abs(i - mu)] * histogram.getCount(i);

			if (bestEntropy > entropy) {
				bestEntropy = entropy;
				bestThreshold = threshold;
			}
		}

		return bestThreshold;
	}
	
	// TODO LICENSE
	// Taken from Auto_Threshold ImageJ plugin
	public int calculateLevelOld( Histogram histogram ) {
		
		// Implements Huang's fuzzy thresholding method 
		// Uses Shannon's entropy function (one can also use Yager's entropy function) 
		// Huang L.-K. and Wang M.-J.J. (1995) "Image Thresholding by Minimizing  
		// the Measures of Fuzziness" Pattern Recognition, 28(1): 41-51
		// M. Emre Celebi  06.15.2007
		// Ported to ImageJ plugin by G. Landini from E Celebi's fourier_0.8 routines
		int threshold=-1;
		int ih, it;
		int first_bin;
		int last_bin;
		int sum_pix;
		int num_pix;
		double term;
		double ent;  // entropy 
		double min_ent; // min entropy 
		double mu_x;

		/* Determine the first non-zero bin */
		first_bin=0;
		for (ih = 0; ih < histogram.size(); ih++ ) {
			if ( histogram.getCount(ih) != 0 ) {
				first_bin = ih;
				break;
			}
		}

		/* Determine the last non-zero bin */
		last_bin=histogram.size() - 1;
		for (ih =histogram.size() - 1; ih >= first_bin; ih-- ) {
			if ( histogram.getCount(ih) != 0 ) {
				last_bin = ih;
				break;
			}
		}
		term = 1.0 / ( double ) ( last_bin - first_bin );
		double [] mu_0 = new double[histogram.size()];
		sum_pix = num_pix = 0;
		for ( ih = first_bin; ih < histogram.size(); ih++ ){
			sum_pix += ih * histogram.getCount(ih);
			num_pix += histogram.getCount(ih);
			/* NUM_PIX cannot be zero ! */
			mu_0[ih] = sum_pix / ( double ) num_pix;
		}

		double [] mu_1 = new double[histogram.size()];
		sum_pix = num_pix = 0;
		for ( ih = last_bin; ih > 0; ih-- ){
			sum_pix += ih * histogram.getCount(ih);
			num_pix += histogram.getCount(ih);
			/* NUM_PIX cannot be zero ! */
			mu_1[ih - 1] = sum_pix / ( double ) num_pix;
		}

		/* Determine the threshold that minimizes the fuzzy entropy */
		threshold = -1;
		min_ent = Double.MAX_VALUE;
		for ( it = 0; it < histogram.size(); it++ ){
			ent = 0.0;
			for ( ih = 0; ih <= it; ih++ ) {
				/* Equation (4) in Ref. 1 */
				mu_x = 1.0 / ( 1.0 + term * Math.abs ( ih - mu_0[it] ) );
				if ( !((mu_x  < 1e-06 ) || ( mu_x > 0.999999))) {
					/* Equation (6) & (8) in Ref. 1 */
					ent += histogram.getCount(ih) * ( -mu_x * Math.log ( mu_x ) - ( 1.0 - mu_x ) * Math.log ( 1.0 - mu_x ) );
				}
			}

			for ( ih = it + 1; ih < histogram.size(); ih++ ) {
				/* Equation (4) in Ref. 1 */
				mu_x = 1.0 / ( 1.0 + term * Math.abs ( ih - mu_1[it] ) );
				if ( !((mu_x  < 1e-06 ) || ( mu_x > 0.999999))) {
					/* Equation (6) & (8) in Ref. 1 */
					ent += histogram.getCount(ih) * ( -mu_x * Math.log ( mu_x ) - ( 1.0 - mu_x ) * Math.log ( 1.0 - mu_x ) );
				}
			}
			/* No need to divide by NUM_ROWS * NUM_COLS * LOG(2) ! */
			if ( ent < min_ent ) {
				min_ent = ent;
				threshold = it;
			}
		}
		return threshold;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Huang){
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
