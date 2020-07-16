/* (C)2020 */
package ch.ethz.biol.cell.imageprocessing.chnl.provider;

abstract class EDTDimensionBase {
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
    private float[] f, z; // NOSONAR
    private int[] y;

    public EDTDimensionBase(int rowWidth) {
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
            for (; ; ) {
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
