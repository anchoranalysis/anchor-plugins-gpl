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
package org.anchoranalysis.plugin.fiji.bean.channel.grayscalereconstruction;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;

/**
 * Greyscale reconstruction, by G.Landini. 30/Oct/2003 for ImageJ
 *
 * <p>Reconstructs a greyscale image (the "mask" image) based on a "seed" image. This is an
 * implementation of the greyscale reconstruction from:
 *
 * <p>Vincent L. Morphological greyscale reconstuction in Image Analysis: Applications and efficient
 * algorithms. IEEE Trans Image Proc 2(2) 176-201, 1993.
 *
 * <p>It is imperative to read Vincent's paper to understand greyscale reconstruction and its
 * applications.
 *
 * <p>The original reconstruction algorithm is: iterated geodesic dilations of the seed UNDER the
 * mask image until stability is reached (the idempotent limit).
 *
 * <p>Send any improvements or bugs to G.Landini@bham.ac.uk
 *
 * <p>v2.0 22/12/2008 Rewrite following the guidelines at
 * http://pacific.mpi-cbg.de/wiki/index.php/PlugIn_Design_Guidelines This version computes the
 * result differently. It binary reconstructs the thresholded mask by the thresholded seed and keeps
 * the maximum greylevel at which the reconstruction was done.
 *
 * <p>Apart from being immensely faster, now it can be called from another plugin without having to
 * show the images. It cannot process stacks anymore, but its use was very limited anyway.
 *
 * <p>v2.1 4/5/2009 4-connected option, fixed skipping empty extreme of histogram v2.2 24/5/2009
 * speed up due to BinaryReconstruct
 */
class GreyscaleReconstruct_ implements PlugIn { // NOSONAR
    /** Ask for parameters and then execute. */
    @Override
    public void run(String arg) { // NOSONAR

        if (IJ.versionLessThan("1.37f")) return;
        int[] wList = WindowManager.getIDList();

        if (wList == null || wList.length < 2) {
            IJ.showMessage("Greyscale Reconstruction", "There must be at least two windows open");
            return;
        }
        String[] titles = new String[wList.length];
        for (int i = 0, k = 0; i < wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (null != imp) titles[k++] = imp.getTitle(); // NOSONAR
        }
        // 1 - Obtain the currently active image if necessary:
        // Erm... no.

        // 2 - Ask for parameters:
        boolean createWindow = true, connect4 = false; // NOSONAR
        GenericDialog gd = new GenericDialog("Greyscale Reconstruction");
        gd.addMessage("Greyscale Reconstruction v 2.2");
        gd.addChoice("mask i1:", titles, titles[0]);
        gd.addChoice("seed i2:", titles, titles[1]);
        gd.addCheckbox("Create New Window", createWindow);
        gd.addCheckbox("4 connected", false);

        gd.showDialog();
        if (gd.wasCanceled()) return;
        // 3 - Retrieve parameters from the dialog
        int i1Index = gd.getNextChoiceIndex();
        int i2Index = gd.getNextChoiceIndex();
        createWindow = gd.getNextBoolean();
        connect4 = gd.getNextBoolean();
        ImagePlus imp1 = WindowManager.getImage(wList[i1Index]);
        ImagePlus imp2 = WindowManager.getImage(wList[i2Index]);

        if (imp1.getStackSize() > 1 || imp2.getStackSize() > 1) {
            IJ.showMessage("Error", "Stacks not supported");
            return;
        }
        if (imp1.getBitDepth() != 8 || imp2.getBitDepth() != 8) {
            IJ.showMessage("Error", "Only 8-bit images are supported");
            return;
        }

        String name = null;

        if (createWindow) name = "Reconstructed";

        // 4 - Execute!
        long start = System.currentTimeMillis();
        Object[] result = exec(imp1, imp2, name, createWindow, connect4);

        // 5 - If all went well, show the image:
        if (null != result) {
            ImagePlus resultImage = (ImagePlus) result[1];
            if (createWindow) resultImage.show();
            else {
                imp2.setProcessor(
                        imp2.getTitle(),
                        resultImage.getProcessor()); // copy the resultImage into the seed image,
            }
        }
        IJ.showStatus(IJ.d2s((System.currentTimeMillis() - start) / 1000.0, 2) + " seconds");
    }

    /**
     * Executes the greyscale reconstruction algorithm.
     *
     * <p>This does NOT show the new image; just returns it.
     *
     * @param imp1 the mask {@link ImagePlus}
     * @param imp2 the seed {@link ImagePlus}
     * @param new_name the name for the new image (if created)
     * @param createWindow if true, creates a new window for the result
     * @param connect4 if true, uses 4-connected neighbors; otherwise, uses 8-connected
     * @return an Object[] array with the name and the reconstructed {@link ImagePlus}
     * @throws IllegalArgumentException if input images are null or not 8-bit
     */
    @SuppressWarnings("unused")
    public Object[] exec(
            ImagePlus imp1,
            ImagePlus imp2,
            String new_name, // NOSONAR
            boolean createWindow,
            boolean connect4) { // NOSONAR

        // 0 - Check validity of parameters
        if (null == imp1) return null; // NOSONAR
        if (null == imp2) return null; // NOSONAR
        if (null == new_name) new_name = imp2.getTitle();

        int width = imp1.getWidth();
        int height = imp1.getHeight();
        ImageProcessor ip1, ip2, ip3, ip4, ip5; // NOSONAR
        ImagePlus imp3, imp4, imp5; // NOSONAR
        ImageStatistics stats;
        int i, j, x, y, size; // NOSONAR
        byte b_0 = (byte) (0 & 0xff); // NOSONAR
        byte b_255 = (byte) (255 & 0xff); // NOSONAR
        IJ.showStatus("Greyscale Reconstruction...");

        // 1 - Perform the magic
        ip1 = imp1.getProcessor();
        ip2 = imp2.getProcessor();
        stats = imp2.getStatistics();

        byte[] pixels1 = (byte[]) ip1.getPixels();
        byte[] pixels2 = (byte[]) ip2.getPixels();
        size = pixels1.length;
        byte[] pixels3 = new byte[size]; // r
        byte[] pixels4 = new byte[size]; // m
        byte[] pixels5 = new byte[size]; // s
        int[] intHisto = new int[256];

        intHisto[255] = stats.histogram[255];

        for (j = 254; j > -1; j--) {
            intHisto[j] = intHisto[j + 1] + stats.histogram[j];
        } // cumulative histogram the way round

        for (j = 0; j < size; j++) {
            pixels3[j] = b_0;
        } // set r accumulator to 0

        ip3 = new ByteProcessor(width, height, pixels3, null);
        imp3 = new ImagePlus(new_name, ip3);

        for (i = 255; i > -1; i--) {
            if (intHisto[i] > 0) {
                System.arraycopy(pixels1, 0, pixels4, 0, size);
                System.arraycopy(pixels2, 0, pixels5, 0, size);
                for (j = 0; j < size; j++) {
                    // Threshold mask
                    pixels4[j] = ((pixels4[j] & 0xff) < i) ? b_0 : b_255;

                    // Threshold seed
                    pixels5[j] = ((pixels5[j] & 0xff) < i) ? b_0 : b_255;
                }

                ip4 = new ByteProcessor(width, height, pixels4, null);
                imp4 = new ImagePlus("_mask", ip4);

                ip5 = new ByteProcessor(width, height, pixels5, null);
                imp5 = new ImagePlus("_seed", ip5);

                BinaryReconstruct_ br = new BinaryReconstruct_();
                /**
                 * Careful! the exec method of BinaryReconstruct does not check whether the images
                 * are binary !!
                 */
                Object[] result = // NOSONAR
                        br.exec(
                                imp4, imp5, null, false, true,
                                connect4); // the result is returned in imp5 if createWindow is
                // false
                //  exec(ImagePlus imp1, ImagePlus imp2, String new_name, boolean createWindow,
                // boolean whiteParticles boolean connect4)
                for (j = 0; j < size; j++) {
                    if ((pixels5[j] & 0xff) == 255)
                        pixels5[j] =
                                (byte) (i & 0xff); // set the thresholded pixels to the greylevel
                    if ((pixels5[j] & 0xff) > (pixels3[j] & 0xff))
                        pixels3[j] = pixels5[j]; // keep always the max greylevel.
                }
                imp4.close();
                imp5.close();
            }
            IJ.showProgress((double) (255 - i) / 255);
        }
        imp3.updateAndDraw();
        // 2 - Return the new name and the image
        return new Object[] {new_name, imp3};
    }
}
