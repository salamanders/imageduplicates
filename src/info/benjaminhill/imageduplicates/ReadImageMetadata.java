/*
 * The MIT License
 *
 * Copyright 2014 benjamin.
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

import info.benjaminhill.pcache.PCacheEntry;
import info.benjaminhill.pcache.ParallelPCache;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.google.common.cache.CacheLoader;
import com.google.common.primitives.Longs;

/**
 * Keeps a DB of images. DB is keyed off of: size, hash of file, hash of thumb
 *
 * @author benjamin
 */
public class ReadImageMetadata {

  private static final Logger LOG = Logger.getAnonymousLogger();

  public static ParallelPCache<PCacheEntry> buildDB() {
    return new ParallelPCache(new CacheLoader<String, PCacheEntry>() {
      @Override
      public PCacheEntry load(final String filePath) {
        final PCacheEntry pi = new PCacheEntry();
        pi.setPk(filePath);
        calcuateImageValues(pi);
        return pi;
      }
    }, "image_metadata");
  }

  private static void calcuateImageValues(final PCacheEntry pi) {
    try {
      final String filePath = pi.getPk();
      LOG.finer(filePath);
      final File f = new File(filePath);
      if (!f.isFile() || !f.canRead()) {
        throw new IllegalArgumentException("Unable to read:" + filePath);
      }

      pi.put("file_h", FileUtils.hashFile(f));
      pi.put("file_len", f.length());
      pi.put("file_name", f.getName());
      pi.put("parent_path_h", FileUtils.hash(f.getParent()));

      final BufferedImage bi = ImageIO.read(f);
      if (bi != null) {
        final int w = bi.getWidth(), h = bi.getHeight();
        pi.put("w", w);
        pi.put("h", h);
        pi.put("r", Math.min(w / (double) h, h / (double) w));

        BufferedImage thumb = ImageUtils.getScaledInstance(bi, ImageFingerprint.MAX_DIM);

        final long imageHash0 = FileUtils.hash(ImageUtils.getData(thumb));
        thumb = ImageUtils.rotate90(thumb);
        final long imageHash90 = FileUtils.hash(ImageUtils.getData(thumb));
        thumb = ImageUtils.rotate90(thumb);
        final long imageHash180 = FileUtils.hash(ImageUtils.getData(thumb));
        thumb = ImageUtils.rotate90(thumb);
        final long imageHash270 = FileUtils.hash(ImageUtils.getData(thumb));

        pi.put("img_h_min", Longs.min(imageHash0, imageHash90, imageHash180, imageHash270));

        final ImageFingerprint ifp = new ImageFingerprint(bi);
        final List<Long> ifpVals = ifp.call();
        for (int i = 0; i < ifpVals.size(); i++) {
          pi.put("imgfp_" + i, ifpVals.get(i));
        }
      }
    } catch (final Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new RuntimeException(ex);
    }
  }

  public static void main(final String... args) throws IOException, Exception {
    final Handler consoleHandler = new ConsoleHandler();
    consoleHandler.setLevel(Level.FINER);
    LOG.addHandler(consoleHandler);

    try (final ParallelPCache<PCacheEntry> pcache = buildDB();) {

      final SortedSet<File> allImageFiles = FileUtils.getFilesByExtensions("jpg", "jpeg", "png");
      System.out.println("Files found: " + allImageFiles.size());
      long numFiles = 0;
      for (final File file : allImageFiles) {

        try {
          pcache.getFuture(file.getAbsolutePath());
        } catch (final Exception ex) {
          System.err.println(file.getAbsolutePath() + " caused " + ex);
        }
        numFiles++;
        /*if (numFiles >= 1_000) {
         System.err.println("Breaking on 1k");
         break;
         }*/
      }
      System.out.println("Waiting for finish");
      pcache.blockForFinish();
    }
  }
}
