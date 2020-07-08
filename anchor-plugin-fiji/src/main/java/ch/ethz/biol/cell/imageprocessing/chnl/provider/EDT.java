package ch.ethz.biol.cell.imageprocessing.chnl.provider;

import java.nio.FloatBuffer;

import org.anchoranalysis.image.binary.BinaryChnl;
import org.anchoranalysis.image.channel.Channel;
import org.anchoranalysis.image.channel.factory.ChannelFactorySingleType;
import org.anchoranalysis.image.voxel.box.VoxelBox;

//
// THIS IS DERIVED FROM Fiji_Plugins.jar in Imagej   EDT.class
//   The license indicates this particular plugin is GPL/PD (Public Domain)
//

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

	public Channel compute( BinaryChnl chnl, ChannelFactorySingleType factory, boolean suppressZ, double multiplyAspectRatio ) {
		
		Channel result = factory.createEmptyInitialised( chnl.getDimensions() );
		
		VoxelBox<FloatBuffer> vbResult = result.getVoxelBox().asFloat();
		
		float zMult = suppressZ ? 1.0f : (float) Math.pow(multiplyAspectRatio,2);
		new EDTDimensionZ(chnl.getVoxelBox(), vbResult, zMult).compute();
		new EDTDimensionY(vbResult,1.0f).compute();
		new EDTDimensionX(vbResult,1.0f).compute();
		return result;
	}
}
