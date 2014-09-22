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

import com.google.common.cache.CacheLoader;
import com.google.common.primitives.Longs;
import info.benjaminhill.pcache.PCacheEntry;
import info.benjaminhill.pcache.ParallelPCache;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;

/**
 * Keeps a DB of images. DB is keyed off of: size, hash of file, hash of thumb
 *
 * @author benjamin
 */
public class ReadImageMetadata {

  private static final Logger LOG = Logger.getLogger(ReadImageMetadata.class.getName());

  public static ParallelPCache<PCacheEntry> buildDB() {
    return new ParallelPCache<>(new CacheLoader<String, PCacheEntry>() {
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

      pi.put("file_h", BetterHash.hashFile(f));
      pi.put("file_len", f.length());
      pi.put("file_name", f.getName());
      pi.put("parent_path_h", BetterHash.hash(f.getParent()));

      final BufferedImage bi = ImageIO.read(f);
      if (bi != null) {
        final int w = bi.getWidth(), h = bi.getHeight();
        pi.put("w", w);
        pi.put("h", h);
        pi.put("r", Math.min(w / (double) h, h / (double) w));

        pi.put("img_h", BetterHash.hash(ImageUtils.getData(bi)));
        
        BufferedImage thumb = ImageUtils.getScaledInstance(bi, ImageFingerprint.MAX_DIM);

        final long imageHash0 = BetterHash.hash(ImageUtils.getData(thumb));
        thumb = ImageUtils.rotate90(thumb);
        final long imageHash90 = BetterHash.hash(ImageUtils.getData(thumb));
        thumb = ImageUtils.rotate90(thumb);
        final long imageHash180 = BetterHash.hash(ImageUtils.getData(thumb));
        thumb = ImageUtils.rotate90(thumb);
        final long imageHash270 = BetterHash.hash(ImageUtils.getData(thumb));

        pi.put("img_h_min", Longs.min(imageHash0, imageHash90, imageHash180, imageHash270));

        final ImageFingerprint ifp = new ImageFingerprint(bi);
        final List<Long> ifpVals = ifp.call();
        for (int i = 0; i < ifpVals.size(); i++) {
          pi.put("imgfp_" + i, ifpVals.get(i));
        }
      }
    } catch(final IIOException | ArrayIndexOutOfBoundsException ex) {
      LOG.log(Level.WARNING, "Unable to read image:{0}", pi.getPk());
    } catch (final Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new RuntimeException(ex);
    }
  }

  public static void main(final String... args) throws IOException, Exception {

    try (final ParallelPCache<PCacheEntry> pcache = buildDB();) {

      final SortedSet<File> allImageFiles = FileUtils.getFilesByExtensions("jpg", "jpeg", "png");
      LOG.log(Level.INFO, "Files found: {0}", allImageFiles.size());
      long numFiles = 0;
      for (final File file : allImageFiles) {

        try {
          pcache.getFuture(file.getAbsolutePath());
        } catch (final Exception ex) {
          LOG.log(Level.WARNING, "{0} caused {1}", new Object[]{file.getAbsolutePath(), ex});
        }
        numFiles++;
        if (numFiles >= Integer.MAX_VALUE) {
          LOG.log(Level.WARNING, "Breaking on {0}", Integer.MAX_VALUE);
          break;
        }
      }
      LOG.info("Waiting for finish");
      pcache.blockForFinish();
    }
  }
}
