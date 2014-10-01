package info.benjaminhill.imageduplicates;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.imgscalr.Scalr;
/*
 * pHash-like image hash.
 * Author: Elliot Shepherd (elliot@jarofworms.com
 * Based On: http://www.hackerfactor.com/blog/index.php?/archives/432-Looks-Like-It.html
 */

public class ImagePHash {

  private static final int SIZE = 32;
  private static final int SIZE_SMALLER = 8;

  private static int getBlue(final BufferedImage img, final int x, final int y) {
    return (img.getRGB(x, y)) & 0xff;
  }

  // DCT function stolen from http://stackoverflow.com/questions/4240490/problems-with-dct-and-idct-algorithm-in-java
  private final double[] c;

  public ImagePHash() {
    c = new double[SIZE];
    for (int i = 1; i < SIZE; i++) {
      c[i] = 1;
    }
    c[0] = 1 / Math.sqrt(2.0);
  }

  // Returns a 'binary string' (like. 001010111011100010) which is easy to do a hamming distance on.
  public byte[] getHash(final BufferedImage img1) throws Exception {

    /* 1. Reduce size.
     * Like Average Hash, pHash starts with a small image.
     * However, the image is larger than 8x8; 32x32 is a good size.
     * This is really done to simplify the DCT computation and not
     * because it is needed to reduce the high frequencies.
     */
    BufferedImage img
            = Scalr.resize(img1, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_EXACT, SIZE, Scalr.OP_GRAYSCALE);

    final double[][] vals = new double[SIZE][SIZE];

    for (int x = 0; x < img.getWidth(); x++) {
      for (int y = 0; y < img.getHeight(); y++) {
        vals[x][y] = getBlue(img, x, y);
      }
    }
    img.flush();
    img = null;

    /* 3. Compute the DCT.
     * The DCT separates the image into a collection of frequencies
     * and scalars. While JPEG uses an 8x8 DCT, this algorithm uses
     * a 32x32 DCT.
     */
    //final long start = System.currentTimeMillis();
    final double[][] dctVals = applyDCT(vals);
    //System.out.println("DCT: " + (System.currentTimeMillis() - start));

    /* 4. Reduce the DCT.
     * This is the magic step. While the DCT is 32x32, just keep the
     * top-left 8x8. Those represent the lowest frequencies in the
     * picture.
     */
    /* 5. Compute the average value.
     * Like the Average Hash, compute the mean DCT value (using only
     * the 8x8 DCT low-frequency values and excluding the first term
     * since the DC coefficient can be significantly different from
     * the other values and will throw off the average).
     */
    double total = 0;

    for (int x = 0; x < SIZE_SMALLER; x++) {
      for (int y = 0; y < SIZE_SMALLER; y++) {
        total += dctVals[x][y];
      }
    }
    total -= dctVals[0][0];

    final double avg = total / ((SIZE_SMALLER * SIZE_SMALLER) - 1);

    /* 6. Further reduce the DCT.
     * This is the magic step. Set the 64 hash bits to 0 or 1
     * depending on whether each of the 64 DCT values is above or
     * below the average value. The result doesn't tell us the
     * actual low frequencies; it just tells us the very-rough
     * relative scale of the frequencies to the mean. The result
     * will not vary as long as the overall structure of the image
     * remains the same; this can survive gamma and color histogram
     * adjustments without a problem.
     */
    final StringBuilder hash = new StringBuilder(SIZE_SMALLER * SIZE_SMALLER);

    for (int x = 0; x < SIZE_SMALLER; x++) {
      for (int y = 0; y < SIZE_SMALLER; y++) {
        if (x != 0 && y != 0) {
          hash.append(dctVals[x][y] > avg ? '1' : '0');
        }
      }
    }
    
    return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(Long.parseLong(hash.toString(), 2)).array();
    // return Long.parseLong(hash.toString(), 2);
  }

  private double[][] applyDCT(final double[][] f) {
    int N = SIZE;

    final double[][] F = new double[N][N];
    for (int u = 0; u < N; u++) {
      for (int v = 0; v < N; v++) {
        double sum = 0.0;
        for (int i = 0; i < N; i++) {
          for (int j = 0; j < N; j++) {
            sum += Math.cos(((2 * i + 1) / (2.0 * N)) * u * Math.PI) * Math.cos(((2 * j + 1) / (2.0 * N)) * v * Math.PI) * (f[i][j]);
          }
        }
        sum *= ((c[u] * c[v]) / 4.0);
        F[u][v] = sum;
      }
    }
    return F;
  }
}
