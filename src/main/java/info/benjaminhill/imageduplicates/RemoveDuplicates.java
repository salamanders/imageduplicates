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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import info.benjaminhill.util.DBLite;
import info.benjaminhill.util.HashUtil;

public class RemoveDuplicates {
  private static final Logger LOG = Logger.getLogger(RemoveDuplicates.class.getName());
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final ImagePHash P_HASH = new ImagePHash();

  public static void logConfig() {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
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

  private static final ImmutableSet<String> IMAGE_EXTENSIONS = ImmutableSet.of("jpg", "jpeg", "png", "tif", "tiff",
      "gif");

  public static void main(final String... args) throws Exception {

    logConfig();

    if (!DBLite.DB.exists()) {
      DBLite.DB.update("drop table if exists image");
      DBLite.DB.update("create table image (" + "path string PRIMARY KEY" + ", path_h BLOB UNIQUE" + ", file_h BLOB"
          + ", path_parent_h BLOB" + ", file_length int" + ", img_width int" + ", img_height int" + ", img_ratio number"
          + ", img_h BLOB" + ", img_percept_h BLOB" + ")");

      final List<File> imageList = Files.walk(getStartingPath()).filter(Files::isRegularFile).filter(Files::isReadable)
          .filter(path -> {
            try {
              return !Files.isHidden(path) && !Files.isDirectory(path);
            } catch (final IOException ex) {
              Throwables.propagate(ex);
            }
            return false;
          }).filter(path -> {
            return IMAGE_EXTENSIONS
                .contains(com.google.common.io.Files.getFileExtension(path.getFileName().toString()).toLowerCase());
          }).map(Path::toFile).collect(Collectors.toList());

      LOG.log(Level.INFO, "Found {0} images", imageList.size());

      imageList.stream().parallel().forEach(f -> {
        int w = 0, h = 0;
        double ratio = 0;
        byte[] img_h = null, p_h = null;
        try {
          final BufferedImage bi = ImageIO.read(f);
          if (bi != null) {
            w = bi.getWidth();
            h = bi.getHeight();
            ratio = Math.min(w / (double) h, h / (double) w);
            img_h = HashUtil.hash(bi);
            p_h = P_HASH.getHash(bi);
            bi.flush();
          }
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }

        DBLite.DB.update("insert into image values(?,?,?,?,?,?,?,?,?,?)", f.toString(), HashUtil.hash(f.toString()),
            HashUtil.hash(f), HashUtil.hash(f.getParent()), f.length(), w, h, ratio, img_h, p_h);
        LOG.info(f.toString());
      });
    }
    
    
    final Table<String, String, Object> images = DBLite.DB.selectTable("select * from image");
    LOG.log(Level.INFO, "Found {0} images.", images.size());

    images.rowMap().entrySet().stream().forEach(img1 -> {
      images.rowMap().entrySet().stream().forEach(img2 -> {

        // Only do 1-way comparisons
        if (img1.getKey().compareTo(img2.getKey()) <= 0) {
          return;
        }

        if (Arrays.equals((byte[]) img1.getValue().get("file_h"), (byte[]) img2.getValue().get("file_h"))) {
          LOG.log(Level.WARNING, "Identical file hash: {0}", img1.getKey() + "," + img2.getKey());
          return;
        }

        if (Arrays.equals((byte[]) img1.getValue().get("img_h"), (byte[]) img2.getValue().get("img_h"))) {
          LOG.log(Level.WARNING, "Identical image hash: {0}", img1.getKey() + "," + img2.getKey());
          return;
        }

        final int dist = HashUtil.hammingDistance((byte[]) img1.getValue().get("img_percept_h"),
            (byte[]) img2.getValue().get("img_percept_h"));
        if (dist < 1) {
          LOG.log(Level.WARNING, "Perceptually close: {0}", img1.getKey() + "," + img2.getKey());
          return;
        }

      });
    });
    // System.out.println(GSON.toJson(.rowMap()));
  }

}
