package org.anchoranalysis.plugin.fiji.bean.channel.provider.distance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Optional;
import org.anchoranalysis.bean.xml.exception.ProvisionFailedException;
import org.anchoranalysis.core.exception.CreateException;
import org.anchoranalysis.core.exception.friendly.AnchorImpossibleSituationException;
import org.anchoranalysis.image.core.channel.Channel;
import org.anchoranalysis.image.core.dimensions.Resolution;
import org.anchoranalysis.image.core.mask.Mask;
import org.anchoranalysis.test.image.MaskFixture;
import org.junit.jupiter.api.Test;

/** Tests {@link DistanceTransform3D}. */
class DistanceTransform3DTest {

    private static final int EXPECTED_2D_WITHOUT_RESOLUTION = 4;
    
    private static final int EXPECTED_3D_WITHOUT_RESOLUTION = 2;
    
    private static final int EXPECTED_2D_WITH_RESOLUTION = 10;
    
    private static final int EXPECTED_3D_WITH_RESOLUTION = 7;
    
    private static final int EXPECTED_3D_WITH_RESOLUTION_Z_ONLY = 3;
    
    private static final int EXPECTED_3D_WITH_RESOLUTION_Z_NAN = EXPECTED_2D_WITH_RESOLUTION;
    
    private static final Resolution RESOLUTION = createResolution(false, false);
    
    private static final Resolution RESOLUTION_Z_ONLY = createResolution(true, false);
    
    private static final Resolution RESOLUTION_Z_NAN = createResolution(false, true);
    
    private static final float Z_RESOLUTION = 2.5f;
    
    @Test
    void test2dWithoutResolution() throws ProvisionFailedException {
        doTest(EXPECTED_2D_WITHOUT_RESOLUTION, false, false, Optional.empty());
    }
    
    @Test
    void test3dWithoutResolution() throws ProvisionFailedException {
        doTest(EXPECTED_3D_WITHOUT_RESOLUTION, true, false, Optional.empty());
    }
    
    @Test
    void test3dWithoutResolution_withSuppressedZ() throws ProvisionFailedException {
        doTest(EXPECTED_2D_WITHOUT_RESOLUTION, true, true, Optional.empty());
    }
    
    @Test
    void test2dWithResolution() throws ProvisionFailedException {
        doTest(EXPECTED_2D_WITH_RESOLUTION, false, false, Optional.of(RESOLUTION));
    }
    
    @Test
    void test3dWithResolution() throws ProvisionFailedException {
        doTest(EXPECTED_3D_WITH_RESOLUTION, true, false, Optional.of(RESOLUTION));
    }
    
    @Test
    void test3dWithResolutionZOnly() throws ProvisionFailedException {
        doTest(EXPECTED_3D_WITH_RESOLUTION_Z_ONLY, true, false, Optional.of(RESOLUTION_Z_ONLY));
    }
    
    @Test
    void test3dWithResolutionZNaN() throws ProvisionFailedException {
        doTest(EXPECTED_3D_WITH_RESOLUTION_Z_NAN, true, false, Optional.of(RESOLUTION_Z_NAN));
    }
    
    private void doTest(double expectedMaxIntensity, boolean do3D, boolean suppressZ, Optional<Resolution> resolution) throws ProvisionFailedException {
        Mask mask = MaskFixture.create(do3D, resolution);
        
        Channel channel = createTransformer(suppressZ).createFromMask(mask);
        
        int expectedMaxAsInteger = (int) Math.ceil(expectedMaxIntensity);
        assertEquals(expectedMaxAsInteger, channel.extract().voxelWithMaxIntensity());        
    }
    
    private static DistanceTransform3D createTransformer(boolean suppressZ) {
        DistanceTransform3D transformer = new DistanceTransform3D();
        transformer.setSuppressZ(suppressZ);
        transformer.setApplyResolution(true);
        return transformer;
    }
    
    private static Resolution createResolution(boolean suppressXY, boolean zNan) {
        try {
            double z = zNan ? Double.NaN : Z_RESOLUTION;
            
            if (suppressXY) {
                return new Resolution(1, 1, z);
            } else {
                return new Resolution(2.2, 3.7, z);
            }
        } catch (CreateException e) {
            throw new AnchorImpossibleSituationException();
        }
    }
}
