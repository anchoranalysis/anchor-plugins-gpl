package ch.ethz.biol.cell.imageprocessing.threshold.calculatelevel;

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


import org.anchoranalysis.core.error.OperationFailedException;
import org.anchoranalysis.image.bean.threshold.calculatelevel.CalculateLevel;
import org.anchoranalysis.image.histogram.Histogram;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

// LICENSE TODO Auto_Threshold.class
public class MinErrorI extends CalculateLevel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	protected static double A(Histogram h, int j) {
		double x = 0;
		for (int i=0;i<=j;i++)
			x+=h.getCount(i);
		return x;
	}

	protected static double B(Histogram h, int j) {
		double x = 0;
		for (int i=0;i<=j;i++)
			x+=i*h.getCount(i);
		return x;
	}

	protected static double C(Histogram h, int j) {
		double x = 0;
		for (int i=0;i<=j;i++)
			x+=i*i*h.getCount(i);
		return x;
	}
	
	@Override
	public int calculateLevel(Histogram h) throws OperationFailedException {

		// Kittler and J. Illingworth, "Minimum error thresholding," Pattern Recognition, vol. 19, pp. 41-47, 1986.
		// C. A. Glasbey, "An analysis of histogram-based thresholding algorithms," CVGIP: Graphical Models and Image Processing, vol. 55, pp. 532-537, 1993.
		// Ported to ImageJ plugin by G.Landini from Antti Niemisto's Matlab code (GPL)
		// Original Matlab code Copyright (C) 2004 Antti Niemisto
		// See http://www.cs.tut.fi/~ant/histthresh/ for an excellent slide presentation
		// and the original Matlab code.

		int threshold = (int) Math.floor( h.mean() );
		int Tprev =-2;
		double mu, nu, p, q, sigma2, tau2, w0, w1, w2, sqterm, temp;
		//int counter=1;
		while (threshold!=Tprev){
			//Calculate some statistics.
			mu = B(h, threshold)/A(h, threshold);
			nu = (B(h, h.size() - 1)-B(h, threshold))/(A(h, h.size() - 1)-A(h, threshold));
			p = A(h, threshold)/A(h, h.size() - 1);
			q = (A(h, h.size() - 1)-A(h, threshold)) / A(h, h.size() - 1);
			sigma2 = C(h, threshold)/A(h, threshold)-(mu*mu);
			tau2 = (C(h, h.size() - 1)-C(h, threshold)) / (A(h, h.size() - 1)-A(h, threshold)) - (nu*nu);

			//The terms of the quadratic equation to be solved.
			w0 = 1.0/sigma2-1.0/tau2;
			w1 = mu/sigma2-nu/tau2;
			w2 = (mu*mu)/sigma2 - (nu*nu)/tau2 + Math.log10((sigma2*(q*q))/(tau2*(p*p)));

			//If the next threshold would be imaginary, return with the current one.
			sqterm = (w1*w1)-w0*w2;
			if (sqterm < 0) {
				//IJ.log("MinError(I): not converging. Try \'Ignore black/white\' options");
				return threshold;
			}

			//The updated threshold is the integer part of the solution of the quadratic equation.
			Tprev = threshold;
			temp = (w1+Math.sqrt(sqterm))/w0;

			if ( Double.isNaN(temp)) {
				//IJ.log ("MinError(I): NaN, not converging. Try \'Ignore black/white\' options");
				threshold = Tprev;
			}
			else
				threshold =(int) Math.floor(temp);
			//IJ.log("Iter: "+ counter+++"  t:"+threshold);
		}
		return threshold;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MinErrorI){
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
