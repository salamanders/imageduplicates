/*
 * The MIT License
 *
 * Copyright 2014 Benjamin Hill.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package info.benjaminhill.imageduplicates;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.imageio.ImageIO;

public class ImageUtils {

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
    final AffineTransform xform = AffineTransform.getQuadrantRotateInstance(1, w / 2, h / 2);
    final Graphics2D g = rot.createGraphics();
    g.drawImage(bi, xform, null);
    g.dispose();
    return rot;
  }

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
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2.drawImage(ret, 0, 0, w, h, null);
      g2.dispose();

      ret = tmp;
    } while (w != targetDim || h != targetDim);

    return ret;
  }

  private ImageUtils() {
    // empty
  }

  /**
   * Heuristic for thumbnail uniqueness should NOT be trusted, yet.
   *
   * @param originalImgFile
   * @param os
   */
  public static void thumbnail(final File originalImgFile, final OutputStream os) {
    try {
      final String thumbFilePath = "./www/cache/" + Math.abs((originalImgFile.getCanonicalPath() + "_" + originalImgFile.length() + "_" + 64).hashCode()) + ".png";
      final File thumbFile = new File(thumbFilePath);

      if (!thumbFile.exists()) {
        BufferedImage thumb = getScaledInstance(ImageIO.read(originalImgFile), 64);
        ImageIO.write(thumb, "png", thumbFile);
      }

      ByteStreams.copy(new FileInputStream(thumbFile), os);

    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static SortedSet<File> getAllImageFiles() throws IOException {

    final SortedSet<File> files = new ConcurrentSkipListSet<>();

    final Path root = Paths.get(System.getProperty("user.home"));

    java.nio.file.Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
        if (dir.toString().contains("iPhoto Library") || dir.toString().toLowerCase().contains("temp")) {
              // TODO: Trash
          // return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
        System.err.printf("Visiting failed for %s\n", file);
        return FileVisitResult.SKIP_SUBTREE;
      }

      @Override
      public FileVisitResult visitFile(final Path filePath, final BasicFileAttributes attrs) {

        switch (Files.getFileExtension(filePath.getFileName().toString().toLowerCase())) {
          case "jpg":
          case "jpeg":
          case "png":
          case "tif":
          case "tiff":
            files.add(filePath.toFile());
            break;
          default:
          // skip
        }
        return FileVisitResult.CONTINUE;
      }
    });

    return files;
  }
}
