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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import com.google.common.base.CharMatcher;

/**
 * Takes any image, and creates a lossy progressive fingerprint of the image based on deltas between
 * the quarters
 *
 * @author Benjamin Hill
 */
public class ImageFingerprint implements Callable<List<Long>> {

  private static final CharMatcher CM_1 = CharMatcher.is('1');

  public static final int MAX_DEPTH = 5;
  // There is some good trade-off between these two values. I don't know what it is.
  public static final int MAX_DIM = 64;

  public static final int MAX_PRINTS = 4;

  private static int avg(final int... vals) {
    long avg = 0;
    for (final int s : vals) {
      avg += s;
    }
    return (int) Math.round(avg / (double) vals.length);
  }

  /**
   * Helper for toByteFP
   */
  private static void finishString(final StringBuilder sb, final List<Long> result) {
    result.add(Long.parseLong(sb.toString(), 2));
    sb.setLength(0);
  }

  /**
   *
   * @param l1
   * @param l2
   * @return
   */
  public static int hammingBinaryDistance(final long l1, final long l2) {
    return CM_1.countIn(Long.toBinaryString(l1 ^ l2));
  }

  public static void main(final String... args) throws Exception {
    for (final String path : new String[] {
        "/Users/benhill/Google Drive/CIOTW/dark/raw_pa/G0036065.JPG",
        "/Users/benhill/Google Drive/CIOTW/dark/raw_pa/G0036066.JPG",
        "/Users/benhill/Google Drive/CIOTW/dark/raw_pa/G0036067.JPG", 
         "/Users/benhill/Pictures/GoPro/G0632004.JPG"
          ,
         "/Users/benhill/Desktop/upload/Summit/IMG_4405_stitch.jpg"
          ,
          "/Users/benhill/Desktop/upload/Summit/G0682300_stitch.jpg"
          ,
          "/Users/benhill/Desktop/upload/Summit/IMG_4366.JPG"
         }) {
      // System.out.println(Arrays.toString(ImageUtils.imageToNormalizedGrayLumData(new File(path),
      // 5)));
      final ImageFingerprint ifp = new ImageFingerprint(ImageIO.read(new File(path)));
      final List<Long> result = ifp.call();
      System.out.println(result);
    }
  }

  /**
   * More compact representation of the signature
   *
   * @param fp
   * @return
   */
  public static List<Long> toByteFP(final Map<Integer, List<Boolean>> fp) {
    final List<Long> result = new ArrayList<>();
    final StringBuilder sb = new StringBuilder(Long.SIZE);
    for (int i = 0; fp.containsKey(i); i++) {
      for (final Boolean b : fp.get(i)) {
        sb.append(b ? '1' : '0');
        if (sb.length() == Long.SIZE - 1) {
          // System.err.println("At depth " + i + " parsing:" + sb.toString());
          finishString(sb, result);
        }
      }
      // finish last one
      if (sb.length() > 0) {
        finishString(sb, result);
      }
    }
    return result;
  }

  final BufferedImage bi;

  /**
   *
   * @param image Already assumed to be square RGB thumbnail with dimension power of 2
   */
  public ImageFingerprint(final BufferedImage bi) {
    this.bi = bi;
  }

  /**
   *
   * @param allLums
   * @param deltas
   * @param currentDim
   * @return average lums for this subsection
   */
  private int calculateLumDeltaStack(final int[] allLums, final Map<Integer, List<Boolean>> deltas,
      final int currentDim, final int currentDepth) {
    // System.err.println("DIM: " + currentDim + " depth:" + currentDepth);

    if (currentDim > 1) {
      // Not there yet, break it up into 4 quads, figure out the quads, then come back.
      final int halfDim = currentDim / 2;

      // UL, UR, LL, LR
      final int[][] quads = new int[4][];

      for (int quadY = 0; quadY <= 1; quadY++) {
        for (int quadX = 0; quadX <= 1; quadX++) {

          final int upperCornerX = quadX * halfDim;
          final int upperCornerY = quadY * halfDim;

          final int quadIdx = quadY * 2 + quadX;
          // System.err.printf(" in quad (%s,%s) dim %s: %n", quadX, quadY, halfDim);
          quads[quadIdx] = new int[halfDim * halfDim];
          for (int targetY = 0; targetY < halfDim; targetY++) {
            for (int targetX = 0; targetX < halfDim; targetX++) {
              final int targetIdx = targetY * halfDim + targetX;
              final int sourceX = targetX + upperCornerX;
              final int sourceY = targetY + upperCornerY;
              final int sourceIdx = sourceY * currentDim + sourceX;
              // System.err.printf("  xfer %s,%s(%s) to %s,%s(%s)%n", sourceX, sourceY, sourceIdx,
              // targetX, targetY, targetIdx);
              quads[quadIdx][targetIdx] = allLums[sourceIdx];
            }
          }
        }
      }

      final int quadAverages[] = new int[4];
      for (int i = 0; i < 4; i++) {
        quadAverages[i] = calculateLumDeltaStack(quads[i], deltas, halfDim, currentDepth + 1);
      }

      // This is the interesting pattern area. What if we just do a diagonal?
      // One across and one down? Divide into 3rds instead of a quad? Focus on the center more?
      // Future optimization: break if too deep and already have enough FP data
      // Current best guess for pattern: left half to right half (over), top to bottom (down)
      if (currentDepth < MAX_DEPTH) {
        if (!deltas.containsKey(currentDepth)) {
          deltas.put(currentDepth, new ArrayList<Boolean>());
        }
        deltas.get(currentDepth).add(
            quadAverages[0] + quadAverages[2] < quadAverages[1] + quadAverages[3]);
        deltas.get(currentDepth).add(
            quadAverages[0] + quadAverages[1] < quadAverages[2] + quadAverages[3]);
      }
    }

    return avg(allLums);
  }

  @Override
  public List<Long> call() throws Exception {
    // Trying without the normalization
    final Map<Integer, List<Boolean>> deltas = new HashMap<>();
    final int[] allLums = ImageUtils.imageToGrayLumData(
            ImageUtils.getScaledInstance(bi, MAX_DIM));
    calculateLumDeltaStack(allLums, deltas, MAX_DIM, 0);
    final List<Long> result = toByteFP(deltas);
    return result.size() < MAX_PRINTS ? result : result.subList(0, MAX_PRINTS);
  }

}
