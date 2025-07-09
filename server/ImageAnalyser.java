import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

public class ImageAnalyser {
    /**
     * 
     * @param content image content
     * @return returns data in the form of [(int) width,(int) height,(String) colorSpace, (int) colorChannels, (boolean) supportsTransparency, (int) bitDepth]
     */
    public Object[] getImageData(byte[] content) {
        try {
            Object[] results = new Object[6];
            ByteArrayInputStream bis = new ByteArrayInputStream(content);
            BufferedImage image = ImageIO.read(bis);

            // Dimensions
            results[0] = image.getWidth();
            results[1] = image.getHeight();

            // Color Space
            int imageColorSpace = image.getColorModel().getColorSpace().getType();
            switch(imageColorSpace) {
                case ColorSpace.TYPE_RGB:
                    results[2] = "RGB";
                    break;
                case ColorSpace.TYPE_CMYK:
                    results[2] = "CMYK";
                    break;
                case ColorSpace.TYPE_GRAY:
                    results[2] = "GrayScale";
                    break;
                case ColorSpace.TYPE_HSV:
                    results[2] = "HSV";
                    break;
                default:
                    results[2] = "Unknown color space";
                    break;
            }

            // Amount of color channels
            results[3] = image.getColorModel().getNumColorComponents();
            // Transparency
            results[4] = image.getColorModel().hasAlpha();
            // Bit Depth
            results[5] = image.getColorModel().getPixelSize();

            return results;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String generateXML(int fileSize, String colorSpace, int width, int height, int colorChannels, boolean hasAlpha, int bitDepth) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
					"<stats>\n" +
					"<type>image</type>\n" +
					"<size>" + fileSize + " B</size>\n" +
					"<colorSpace>"  + colorSpace + "</colorSpace>\n" +
					"<width>" + width + " px</width>\n" +
					"<height>" + height + " px</height>\n" +
                    "<colorChannels>" + colorChannels + "</colorChannels>\n" +
                    "<supportsTransparency>" + hasAlpha + "</supportsTransparency>\n" +
                    "<bitDepth>" + bitDepth + "</bitDepth>\n" +
					"</stats>";
		return xml;
    }
}
