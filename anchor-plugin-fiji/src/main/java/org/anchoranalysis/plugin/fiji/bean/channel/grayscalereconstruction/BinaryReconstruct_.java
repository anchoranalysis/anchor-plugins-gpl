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
 * Binary reconstruction, by G.Landini. 22/Oct/2003 for ImageJ
 *
 * <p>Reconstructs binary objects of the "mask" image, based on markers present in the "seed" image.
 * The result is sent to the seed image or to a new image named Reconstructed if so chosen in the
 * menu.
 *
 * <p>It can handle black [0] particles on white [255] background or white particles on a black
 * background.
 *
 * <p>Binary reconstruction removes binary objects from the mask image which have no markers in the
 * seed image.
 *
 * <p>The reconstruction algorithm is: dilation (or erosion for white particles) of the seed image
 * AND mask image until there are no more pixles added by succesive dilations (this is called the
 * idempotent limit).
 *
 * <p>It supports stacks reconstructed by a single seed image and also a single image reconstructed
 * by a stack of seeds. Not tested in versions older than 1.31l
 *
 * <p>Send any improvements or bugs to G.Landini@bham.ac.uk
 *
 * <p>v1.0 release v1.1 2/11/2003 replaced IJ erosion/dilations with own version as IJ does not
 * process borders v1.2 3/11/2003 small speed up v1.3 16/4/2004 small speed up, feedback when
 * reconstructing v1.4 20/11/2004 ~x4 speedup with new dilation v1.5 24/5/2006 changed 'dilations in
 * a mask' for 'floodfill8 in the mask from the seed' v2.0 22/12/2008 Rewrite following the
 * guidelines at http://pacific.mpi-cbg.de/wiki/index.php/PlugIn_Design_Guidelines Apart from being
 * faster, now it can be called from another plugin without having to show the images. It cannot
 * process stacks anymore, but its use was very limited anyway. v2.1 4/5/2009 4-connected option
 * v2.2 25/5/2009 almost 1.5x speedup by using 1D arrays
 *
 * <p>To call this plugin from another without having to display the images use for example:
 *
 * <p>BinaryReconstruct_ br = new BinaryReconstruct_(); Object[] result = br.exec(img1, img2, null,
 * false, true, false ); //parameters above are: mask ImagePlus, seed ImagePlus, name, create new
 * image, white particles, connect4 if (null != result) { String name = (String) result[0];
 * ImagePlus recons = (ImagePlus) result[1]; }
 */
class BinaryReconstruct_ implements PlugIn { // NOSONAR
    /** Ask for parameters and then execute. */
    @Override
    public void run(String arg) { // NOSONAR

        if (IJ.versionLessThan("1.37f")) return;
        int[] wList = WindowManager.getIDList();

        if (wList == null || wList.length < 2) {
            IJ.showMessage("Binary Reconstruction", "There must be at least two windows open");
            return;
        }
        String[] titles = new String[wList.length];
        for (int i = 0, k = 0; i < wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (null != imp) titles[k++] = imp.getTitle(); // NOSONAR
        }
        // 1 - Obtain the currently active image if necessary:

        // 2 - Ask for parameters:
        boolean createWindow = true,
                whiteParticles = Prefs.blackBackground, // NOSONAR
                connect4 = false; // NOSONAR
        GenericDialog gd = new GenericDialog("Binary Reconstruction");
        gd.addMessage("Binary Reconstruction v 2.2");
        gd.addChoice("mask :", titles, titles[0]);
        gd.addChoice("seed :", titles, titles[1]);
        gd.addCheckbox("Create New Window", createWindow);
        gd.addCheckbox("White particles on black background", whiteParticles);
        gd.addCheckbox("4 connected", false);
        gd.showDialog();
        if (gd.wasCanceled()) return;
        // 3 - Retrieve parameters from the dialog
        int i1Index = gd.getNextChoiceIndex();
        int i2Index = gd.getNextChoiceIndex();
        createWindow = gd.getNextBoolean();
        whiteParticles = gd.getNextBoolean();
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

        ImageStatistics stats;

        // Check images are binary. You must check this before calling the exec method.
        //
        stats = imp1.getStatistics();
        if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount) {
            IJ.error("8-bit binary mask image (0 and 255) required.");
            return;
        }
        stats = imp2.getStatistics();
        if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount) {
            IJ.error("8-bit binary seed image (0 and 255) required.");
            return;
        }

        String name = null;

        if (createWindow) name = "Reconstructed";

        // 4 - Execute!
        Object[] result = exec(imp1, imp2, name, createWindow, whiteParticles, connect4);

        // 5 - If all went well, show the image:
        if (null != result) {
            ImagePlus resultImage = (ImagePlus) result[1];

            if (createWindow) resultImage.show();
            else imp2.getProcessor().insert(resultImage.getProcessor(), 0, 0);
        }
    }

    /**
     * Executes the binary reconstruction algorithm.
     *
     * <p>Warning!: This method does NOT check whether the two input images are binary, this is
     * checked in the setup, so be careful when calling this method from another plugin. Make sure
     * both images are binary!! Does NOT show the new, image; just returns it.
     *
     * @param imp1 the mask {@link ImagePlus}
     * @param imp2 the seed {@link ImagePlus}
     * @param new_name the name for the new image (if created)
     * @param createWindow if true, creates a new window for the result
     * @param whiteParticles if true, assumes white particles on black background
     * @param connect4 if true, uses 4-connected neighbors; otherwise, uses 8-connected
     * @return an Object[] array with the name and the reconstructed {@link ImagePlus}
     * @throws IllegalArgumentException if input images are null or not binary
     */
    @SuppressWarnings("unused")
    public Object[] exec(
            ImagePlus imp1,
            ImagePlus imp2,
            String new_name,
            boolean createWindow,
            boolean whiteParticles,
            boolean connect4) { // NOSONAR

        // 0 - Check validity of parameters
        if (null == imp1) return null; // NOSONAR
        if (null == imp2) return null; // NOSONAR
        if (null == new_name) new_name = imp2.getTitle();

        int width = imp1.getWidth();
        int height = imp1.getHeight();
        int size = width * height;
        ImageProcessor ip1, ip3; // NOSONAR
        ImagePlus imp3;

        int x, y, offset, pointer; // NOSONAR
        int foreground = 255, background = 0; // NOSONAR
        byte bf = (byte) 0xff, bb = (byte) 0, b127 = (byte) (127 & 0xff); // NOSONAR

        IJ.showStatus("Binary Reconstruction...");

        ip1 = imp1.getProcessor();

        // 1 - Perform the magic
        if (createWindow) {
            imp3 = new ImagePlus(new_name, imp2.getProcessor().duplicate());
            ip3 = imp3.getProcessor();
        } else {
            imp3 = imp2;
            ip3 = imp2.getProcessor();
        }

        if (!whiteParticles) {
            foreground = 0;
            background = 255; // NOSONAR
            bf = (byte) 0; // NOSONAR
            bb = (byte) 0xff;
        }

        byte[] pixel = (byte[]) ip1.getPixels();
        byte[] seed = new byte[size];
        byte[] res = (byte[]) ip3.getPixels();

        for (y = 0; y < size; y++) {
            seed[y] = res[y];
            if ((pixel[y] & 0xff) == foreground) res[y] = b127; // put mask as 127
            else res[y] = bb; // put background
        }

        FloodFiller ff = new FloodFiller(ip3);
        ip3.setColor(foreground);

        if (connect4) {
            for (y = 0; y < height; y++) {
                offset = y * width;
                for (x = 0; x < width; x++) {
                    pointer = offset + x;
                    if ((seed[pointer] & 0xff) == foreground) {
                        if ((res[pointer] & 0xff) == 127) ff.fill(x, y); // NOSONAR
                    }
                }
            }
        } else { // 8
            for (y = 0; y < height; y++) {
                offset = y * width;
                for (x = 0; x < width; x++) {
                    pointer = offset + x;
                    if ((seed[pointer] & 0xff) == foreground) {
                        if ((res[pointer] & 0xff) == 127) ff.fill8(x, y); // NOSONAR
                    }
                }
            }
        }
        // clean not seeded mask
        for (y = 0; y < size; y++) {
            if ((res[y] & 0xff) == 127) res[y] = bb; // erase mask
        }

        imp3.updateAndDraw();

        // 2 - Return the new name and the image
        return new Object[] {new_name, imp3};
    }
}
