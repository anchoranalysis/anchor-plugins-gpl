/*-
 * #%L
 * anchor-plugin-fiji
 * %%
 * Copyright (C) 2010 - 2020 Owen Feehan, ETH Zurich, University of Zurich, Hoffmann-La Roche
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

package org.anchoranalysis.plugin.fiji.bean.channel.provider.distance;

/**
 * Base class for computing Euclidean Distance Transform along a single dimension.
 */
abstract class EDTDimensionBase {
    private int extent;
    /*
     * parabola k is defined by y[k] (v in the paper)
     * and f[k] (f(v[k]) in the paper): (y, f) is the
     * coordinate of the minimum of the parabola.
     * z[k] determines the left bound of the interval
     * in which the k-th parabola determines the lower
     * envelope.
     */

    private int k; // NOSONAR
    private float[] f, z; // NOSONAR
    private int[] y;

    /**
     * Constructs an EDTDimensionBase with a given extent.
     *
     * @param extent the extent of the dimension being processed
     */
    protected EDTDimensionBase(int extent) {
        this.extent = extent;
        f = new float[extent + 1];
        z = new float[extent + 1];
        y = new int[extent + 1];
    }

    /**
     * Computes the Euclidean Distance Transform for all rows in this dimension.
     */
    public final void compute() {
        while (nextRow()) {
            computeRow();
        }
    }

    /**
     * Gets the value at a specific column in the current row.
     *
     * @param column the column index
     * @return the value at the specified column
     */
    protected abstract float get(int column);

    /**
     * Sets the value at a specific column in the current row.
     *
     * @param column the column index
     * @param value the value to set
     */
    protected abstract void set(int column, float value);

    /**
     * Gets the multiplication constant for this dimension.
     *
     * @return the multiplication constant
     */
    protected abstract float getMultiplyConstant();

    /**
     * Moves to the next row for processing.
     *
     * @return true if there is a next row to process, false otherwise
     */
    protected abstract boolean nextRow();

    private final void computeRow() {
        // calculate the parabolae ("lower envelope")
        f[0] = Float.MAX_VALUE;
        y[0] = -1;
        z[0] = Float.MAX_VALUE;
        k = 0;
        float fx, s; // NOSONAR
        for (int x = 0; x < extent; x++) {
            fx = get(x);
            for (; ; ) { // NOSONAR
                // calculate the intersection
                s = ((fx + x * x) - (f[k] + y[k] * y[k])) / 2 / (x - y[k]);
                if (s > z[k]) break;
                if (--k < 0) break;
            }
            k++;
            y[k] = x;
            f[k] = fx;
            z[k] = s;
        }
        z[++k] = Float.MAX_VALUE;
        // calculate g(x)
        int i = 0;
        for (int x = 0; x < extent; x++) {
            while (z[i + 1] < x) {
                i++;
            }
            set(x, getMultiplyConstant() * (x - y[i]) * (x - y[i]) + f[i]);
        }
    }
}
