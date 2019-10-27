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
import java.nio.FloatBuffer;

import org.anchoranalysis.core.error.CreateException;
import org.anchoranalysis.image.binary.BinaryChnl;
import org.anchoranalysis.image.chnl.Chnl;
import org.anchoranalysis.image.chnl.factory.ChnlFactorySingleType;
import org.anchoranalysis.image.extent.Extent;
import org.anchoranalysis.image.voxel.box.VoxelBox;

//
// THIS IS DERIVED FROM Fiji_Plugins.jar in Imagej   EDT.class
//   The license indicates this particular plugin is GPL/PD (Public Domain)
//

// TODO LICENSE LICENSE LICENSE

// CHECK LICENSE
/*
 * The idea of the Euclidean Distance Transform is to get the
 * distance of every outside pixel to the nearest outside pixel.
 *
 * We use the algorithm proposed in

	@TECHREPORT{Felzenszwalb04distancetransforms,
	    author = {Pedro F. Felzenszwalb and Daniel P. Huttenlocher},
	    title = {Distance transforms of sampled functions},
	    institution = {Cornell Computing and Information Science},
	    year = {2004}
	}

 * Felzenszwalb & Huttenlocher's idea is to extend the concept to
 * a broader one, namely to minimize not only the distance to an
 * outside pixel, but to minimize the distance plus a value that
 * depends on the outside pixel.
 *
 * In mathematical terms: we determine the minimum of the term
 *
 *	g(x) = min(d^2(x, y) + f(y) for all y)
 *
 * where y runs through all pixels and d^2 is the square of the
 * Euclidean distance. For the Euclidean distance transform, f(y)
 * is 0 for all outside pixels, and infinity for all inside
 * pixels, and the result is the square root of g(x).
 *
 * The trick is to calculate g in one dimension, store the result,
 * and use it as f(y) in the next dimension. Continue until you
 * covered all dimensions.
 *
 * In order to find the minimum in one dimension (i.e. row by
 * row), the following fact is exploited: for two different
 * y1 < y2, (x - y1)^2 + f(y1) < (x - y2)^2 + f(y2) for x < s,
 * where s is the intersection point of the two parabolae (there
 * is the corner case where one parabola is always lower than
 * the other one, in that case there is no intersection).
 *
 * Using this fact, for each row of n elements, a maximum number
 * of n parabolae are constructed, adding them one by one for each
 * y, adjusting the range of x for which this y yields the minimum,
 * possibly overriding a number of previously added parabolae.
 *
 * At most n parabolae can be added, so the complexity is still
 * linear.
 *
 * After this step, the list of parabolae is iterated to calculate
 * the values for g(x).
 */
class EDT  {

	public Chnl compute( BinaryChnl chnl, ChnlFactorySingleType factory, boolean suppressZ, double multiplyAspectRatio ) throws CreateException {
		
		Chnl result = factory.createEmptyInitialised( chnl.getDimensions() );
		
		VoxelBox<FloatBuffer> vbResult = result.getVoxelBox().asFloat();
		
		float zMult = suppressZ ? 1.0f : (float) Math.pow(multiplyAspectRatio,2);
		new Z(chnl.getVoxelBox(), vbResult, zMult).compute();
		new Y(vbResult,1.0f).compute();
		new X(vbResult,1.0f).compute();
		return result;
	}

	private static abstract class EDTBase {
		private int width;
		/*
		 * parabola k is defined by y[k] (v in the paper)
		 * and f[k] (f(v[k]) in the paper): (y, f) is the
		 * coordinate of the minimum of the parabola.
		 * z[k] determines the left bound of the interval
		 * in which the k-th parabola determines the lower
		 * envelope.
		 */

		private int k;
		private float[] f, z;
		private int[] y;

		public EDTBase(int rowWidth) {
			width = rowWidth;
			f = new float[width + 1];
			z = new float[width + 1];
			y = new int[width + 1];
		}

		public final void computeRow() {
			// calculate the parabolae ("lower envelope")
			f[0] = Float.MAX_VALUE;
			y[0] = -1;
			z[0] = Float.MAX_VALUE;
			k = 0;
			float fx, s;
			for (int x = 0; x < width; x++) {
				fx = get(x);
				for (;;) {
					// calculate the intersection
					s = ((fx + x * x) - (f[k] + y[k] * y[k])) / 2 / (x - y[k]);
					if (s > z[k])
						break;
					if (--k < 0)
						break;
				}
				k++;
				y[k] = x;
				f[k] = fx;
				z[k] = s;
			}
			z[++k] = Float.MAX_VALUE;
			// calculate g(x)
			int i = 0;
			for (int x = 0; x < width; x++) {
				while (z[i + 1] < x) {
					i++;
				}
				set(x, getMultiplyConstant() * (x - y[i]) * (x - y[i]) + f[i]);
			}
		}

		public abstract float get(int column);

		public abstract void set(int column, float value);
		
		public abstract float getMultiplyConstant();

		public final void compute() {
			while (nextRow()) {
				computeRow();
			}
		}

		public abstract boolean nextRow();
	}

	private static class Z extends EDTBase {
		private byte[][] inSlice;
		private float[][] outSlice;
		private int offset;
		private int bufferXYSize;
		
		private float multiplyConstant;

		public Z(VoxelBox<ByteBuffer> in, VoxelBox<FloatBuffer> out, float multiplyConstant) {
			super( in.extnt().getZ() );
			
			this.multiplyConstant = multiplyConstant;
			
			int d = in.extnt().getZ();
			
			bufferXYSize = in.extnt().getVolumeXY();
			
			inSlice = new byte[d][];
			outSlice = new float[d][];
			for (int i = 0; i < d; i++) {
				inSlice[i] = (byte[])in.getPixelsForPlane(i).buffer().array();
				outSlice[i] = (float[])out.getPixelsForPlane(i).buffer().array();
			}
			offset = -1;
		}

		public final float get(int x) {
			return inSlice[x][offset] == 0 ? 0 : Float.MAX_VALUE;
		}

		public final void set(int x, float value) {
			outSlice[x][offset] = value;
		}

		public final boolean nextRow() {
			return ++offset < bufferXYSize;
		}
		
		public final float getMultiplyConstant() {
			return multiplyConstant;
		}
	}

	private static abstract class OneDimension extends EDTBase {
		
		private VoxelBox<FloatBuffer> stack;
		private FloatBuffer slice;
		private int offset, lastOffset, rowStride, columnStride, sliceIndex;

		public OneDimension(VoxelBox<FloatBuffer> out, boolean iterateX) {
			super(iterateX ? out.extnt().getX() : out.extnt().getY() );
			stack = out;
			
			Extent e = out.extnt();
			
			columnStride = iterateX ? 1 : e.getX();
			rowStride = iterateX ? e.getX() : 1;
			offset = e.getVolumeXY();
			lastOffset = rowStride * (iterateX ? e.getY() : e.getX());
			sliceIndex = -1;
		}

		public final float get(int x) {
			return slice.get(x * columnStride + offset);
		}

		public final boolean nextRow() {
			offset += rowStride;
			if (offset >= lastOffset) {
				if (++sliceIndex >= stack.extnt().getZ())
					return false;
				offset = 0;
				slice = stack.getPixelsForPlane(sliceIndex).buffer();
			}
			return true;
		}

		protected void putIntoPuffer( int x, float value ) {
			slice.put( x * columnStride + offset, value );
		}
	}

	private class Y extends OneDimension {
		
		private float multiplyConstant;
		
		public Y(VoxelBox<FloatBuffer> out, float multiplyConstant) {
			super(out, false);
			this.multiplyConstant = multiplyConstant;
		}

		public final void set(int x, float value) {
			putIntoPuffer( x, value );
		}
		
		public final float getMultiplyConstant() {
			return multiplyConstant;
		}
	}

	private class X extends OneDimension {
		
		private float multiplyConstant;
		
		public X(VoxelBox<FloatBuffer> out, float multiplyConstant) {
			super(out, true);
			this.multiplyConstant = multiplyConstant;
		}

		public final void set(int x, float value) {
			putIntoPuffer(x, (float)Math.sqrt(value));
		}
		
		public final float getMultiplyConstant() {
			return multiplyConstant;
		}
	}

}
