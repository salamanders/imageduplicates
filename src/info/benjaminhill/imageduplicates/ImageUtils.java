/*
 * The MIT License
 *
 * Copyright 2014 Benjamin Hill.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package info.benjaminhill.imageduplicates;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.logging.Logger;

import com.google.common.primitives.Ints;

public class ImageUtils {

  private static final Logger LOG = Logger.getLogger(ImageUtils.class.getName());

  /**
   * Normally not needed image to integer pixel data
   *
   * @param bi
   * @return
   */
  public static int[] getData(final BufferedImage bi) {
    BufferedImage tmp = bi;
    if (bi.getType() != BufferedImage.TYPE_INT_RGB) {
      tmp = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
      tmp.getGraphics().drawImage(bi, 0, 0, null);
    }
    final DataBufferInt db1 = (DataBufferInt) tmp.getRaster().getDataBuffer();
    assert db1.getNumBanks() == 1;
    return db1.getData();
  }

  /**
   * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
   * https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
   *
   * @param img the original image to be scaled
   * @param targetDim the desired width of the scaled instance, in pixels
   * @return a scaled version of the original {@code BufferedImage}
   */
  public static BufferedImage getScaledInstance(final BufferedImage img, final int targetDim) {
    if (img == null) {
      throw new IllegalArgumentException("Null image");
    }

    BufferedImage ret = img;
    int w, h;

    // Use multi-step technique: start with original size, then
    // scale down in multiple passes with drawImage()
    // until the target size is reached
    w = img.getWidth();
    h = img.getHeight();

    do {
      if (w > targetDim) {
        w /= 2;
      }
      if (w < targetDim) {
        w = targetDim;
      }
      if (h > targetDim) {
        h /= 2;
      }
      if (h < targetDim) {
        h = targetDim;
      }

      final BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      final Graphics2D g2 = tmp.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2.drawImage(ret, 0, 0, w, h, null);
      g2.dispose();

      ret = tmp;
    } while (w != targetDim || h != targetDim);

    return ret;
  }

  /**
   *
   * From http://www.tomgibara.com/computer-vision/CannyEdgeDetector.java
   */
  private static int[] imageToGrayLumData(final BufferedImage sourceImage) {
    final int width = sourceImage.getWidth();
    final int height = sourceImage.getHeight();
    final int[] data = new int[width * height];

    switch (sourceImage.getType()) {
      case BufferedImage.TYPE_INT_RGB:
      case BufferedImage.TYPE_INT_ARGB: {
        final int[] pixels =
            (int[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
        assert pixels.length == data.length;
        for (int i = 0; i < data.length; i++) {
          data[i] = luminance(pixels[i]);
        }
      }
      break;

      case BufferedImage.TYPE_BYTE_GRAY: {
        final byte[] pixels =
            (byte[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
        assert pixels.length == data.length;
        for (int i = 0; i < data.length; i++) {
          data[i] = pixels[i] & 0xff;
        }
      }
      break;
      case BufferedImage.TYPE_USHORT_GRAY: {
        final short[] pixels =
            (short[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
        assert pixels.length == data.length;
        for (int i = 0; i < data.length; i++) {
          data[i] = (pixels[i] & 0xffff) / 256;
        }
      }
      break;
      case BufferedImage.TYPE_3BYTE_BGR: {
        final byte[] pixels =
            (byte[]) sourceImage.getData().getDataElements(0, 0, width, height, null);
        int offset = 0;
        for (int i = 0; i < data.length; i++) {
          final int b = pixels[offset++] & 0xff;
          final int g = pixels[offset++] & 0xff;
          final int r = pixels[offset++] & 0xff;
          data[i] = luminance(r, g, b);
        }
      }
      break;
      default:
        throw new IllegalArgumentException("Unsupported image type: " + sourceImage.getType());
    }
    return data;
  }

  /**
   * Very simple stretch of luminance integer pixels to 0 to 255
   *
   * @param data
   * @return
   */
  public static short[] imageToNormalizedGrayLumData(final BufferedImage bi, final int dim) {
    final int[] data = imageToGrayLumData(getScaledInstance(bi, dim));
    final int min = Ints.min(data), max = Ints.max(data);
    final double multiplier = 255d / (max - min);
    final short[] result = new short[data.length];
    for (int i = 0; i < data.length; i++) {
      result[i] = (short) Math.round(multiplier * (data[i] - min));
    }
    return result;
  }

  /**
   * From http://www.tomgibara.com/computer-vision/CannyEdgeDetector.java
   *
   * @param r
   * @param g
   * @param b
   * @return
   */
  private static int luminance(final float r, final float g, final float b) {
    return (int) Math.round(0.299d * r + 0.587d * g + 0.114d * b);
  }

  private static int luminance(final int rgb) {
    return luminance((rgb & 0xff0000) >> 16, (rgb & 0xff00) >> 8, rgb & 0xff);
  }

  /**
   * Should be used to rotate already small images (thumbnails)
   *
   * @param bi
   * @return
   */
  public static BufferedImage rotate90(final BufferedImage bi) {
    final int w = bi.getWidth();
    final int h = bi.getHeight();
    final BufferedImage rot = new BufferedImage(h, w, bi.getType());
    final AffineTransform xform = AffineTransform.getQuadrantRotateInstance(1, w / 2.0, h / 2.0);
    final Graphics2D g = rot.createGraphics();
    g.drawImage(bi, xform, null);
    g.dispose();
    return rot;
  }

  /*
   * public static BufferedImage convertToGrayScale(final BufferedImage image) { BufferedImage
   * result = new BufferedImage( image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
   * Graphics2D g = (Graphics2D) result.getGraphics(); g.drawImage(image, 0, 0, null); g.dispose();
   * return result; }
   */
  private ImageUtils() {
    // empty
  }

}
