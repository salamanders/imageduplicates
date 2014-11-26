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

import com.google.common.cache.CacheLoader;
import info.benjaminhill.pcache.PCache;
import info.benjaminhill.pcache.PCacheEntry;
import info.benjaminhill.pcache.ParallelPCache;
import info.benjaminhill.util.HashUtil;
import info.benjaminhill.util.RecursiveFileFind;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

/**
 *
 * @author Benjamin Hill
 */
public class CollectStats {

  private static final Logger LOG = Logger.getLogger(CollectStats.class.getName());
  private static final ImagePHash PHASH = new ImagePHash();
  public static final String IMG_HASH_PERCEPT = "i.hp", IMG_HASH_FULL = "i.hf", FILE_PATH_PARENT_HASH = "f.p.h", FILE_NAME = "f.n", FILE_LENGTH = "f.l", FILE_HASH = "f.h", IMAGE_RATIO = "i.r", IMAGE_HEIGHT = "i.h", IMAGE_WIDTH = "i.w";

  public static void logConfig() {
    final Logger topLogger = java.util.logging.Logger.getLogger("");
    Handler consoleHandler = null;
    for (final Handler handler : topLogger.getHandlers()) {
      if (handler instanceof ConsoleHandler) {
        consoleHandler = handler;
        break;
      }
    }
    if (consoleHandler == null) {
      consoleHandler = new ConsoleHandler();
      topLogger.addHandler(consoleHandler);
    }
    consoleHandler.setLevel(java.util.logging.Level.ALL);
  }

  private static Path getStartingPath() {
    final JFileChooser jfc = new JFileChooser();
    jfc.setDialogTitle("Choose the folder to scan");
    jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    jfc.setApproveButtonText("Start in this Folder");
    final int returnVal = jfc.showOpenDialog(null);
    if (returnVal != JFileChooser.APPROVE_OPTION) {
      throw new RuntimeException("No directory chosen.");
    }
    return Paths.get(jfc.getSelectedFile().toURI());
  }

  public static void main(final String... args) throws IOException, Exception {
    logConfig();
    final SortedSet<File> images = new RecursiveFileFind().walkFrom(getStartingPath());
    LOG.log(Level.INFO, "Found {0} images.", images.size());
    readImageData(images);
  }

  private static void readImageData(final SortedSet<File> allImageFiles) {
    try (final ParallelPCache<PCacheEntry> pcache = buildDB();) {
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

      pi.put(FILE_HASH, HashUtil.hash(f));
      pi.put(FILE_LENGTH, f.length());
      pi.put(FILE_NAME, f.getName());
      pi.put(FILE_PATH_PARENT_HASH, HashUtil.hash(f.getParent()));

      final BufferedImage bi = ImageIO.read(f);
      if (bi != null) {
        final int w = bi.getWidth(), h = bi.getHeight();
        pi.put(IMAGE_WIDTH, w);
        pi.put(IMAGE_HEIGHT, h);
        pi.put(IMAGE_RATIO, Math.min(w / (double) h, h / (double) w));
        
        pi.put(IMG_HASH_FULL, HashUtil.hash(bi));
        pi.put(IMG_HASH_PERCEPT, PHASH.getHash(bi));
        bi.flush();

        //BufferedImage thumb = Scalr.resize(bi, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_EXACT, 32, Scalr.OP_GRAYSCALE);
      }
    } catch (final IIOException | ArrayIndexOutOfBoundsException ex) {
      LOG.log(Level.WARNING, "Unable to read image:{0} {1}", new Object[]{pi.getPk(), ex});
    } catch (final Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      LOG.log(Level.SEVERE, "Issue with:{0}", pi.getPk());
      throw new RuntimeException(ex);
    }
  }

}
