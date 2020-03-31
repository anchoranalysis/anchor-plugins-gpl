package org.anchoranalysis.plugin.fiji.bean.threshold.calculatelevel;

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


import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.bean.threshold.calculatelevel.CalculateLevel;
import org.anchoranalysis.image.histogram.Histogram;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Intermodes extends CalculateLevel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// TODO LICENSE
	@Override
	public int calculateLevel(Histogram h) throws OperationFailedException {

		// J. M. S. Prewitt and M. L. Mendelsohn, "The analysis of cell images," in
		// Annals of the New York Academy of Sciences, vol. 128, pp. 1035-1053, 1966.
		// ported to ImageJ plugin by G.Landini from Antti Niemisto's Matlab code (GPL)
		// Original Matlab code Copyright (C) 2004 Antti Niemisto
		// See http://www.cs.tut.fi/~ant/histthresh/ for an excellent slide presentation
		// and the original Matlab code.
		//
		// Assumes a bimodal histogram. The histogram needs is smoothed (using a
		// running average of size 3, iteratively) until there are only two local maxima.
		// j and k
		// Threshold t is (j+k)/2.
		// Images with histograms having extremely unequal peaks or a broad and
		// ﬂat valley are unsuitable for this method.
		double [] iHisto = new double [h.size()];
		int iter =0;
		int threshold=-1;
		for (int i=0; i<h.size(); i++) {
			iHisto[i]=(double) h.getCount(i);
		}
		double [] tHisto = iHisto;

		while (!bimodalTest(iHisto) ) {
			 //smooth with a 3 point running mean filter
			for (int i=1; i<h.size() - 1; i++) {
				tHisto[i]= (iHisto[i-1] + iHisto[i] + iHisto[i+1])/3;
			}
			tHisto[0] = (iHisto[0]+iHisto[1])/3; //0 outside
			tHisto[h.size() - 1] = (iHisto[h.size() - 2]+iHisto[h.size() - 1])/3; //0 outside
			iHisto = tHisto;
			iter++;
			if (iter>10000) {
				throw new OperationFailedException("No threshold found after 10000 iterations");
			}
		}

		// The threshold is the mean between the two peaks.
		int tt=0;
		for (int i=1; i<h.size() - 1; i++) {
			if (iHisto[i-1] < iHisto[i] && iHisto[i+1] < iHisto[i]){
				tt += i;
			}
		}
		threshold = (int) Math.floor(tt/2.0);
		return threshold;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Intermodes){
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
	
	// From ImageJ
	private static boolean bimodalTest(double [] y) {
		int len=y.length;
		boolean b = false;
		int modes = 0;
 
		for (int k=1;k<len-1;k++){
			if (y[k-1] < y[k] && y[k+1] < y[k]) {
				modes++;
				if (modes>2) {
					return false;
				}
			}
		}
		if (modes == 2) {
			b = true;
		}
		return b;
	}
}
