/*
 * The MIT License
 *
 * Copyright 2014 benjamin.
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

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

/**
 * Keeps a DB of images. DB is keyed off of: size, hash of file, hash of thumb
 *
 * @author benjamin
 */
public class Main {

  public static void main(final String... args) throws IOException {
    try (final MiniImageDB db = new MiniImageDB()) {
      final SortedSet<File> allImageFiles = ImageUtils.getAllImageFiles();
      System.out.println("Files found: " + allImageFiles.size());
      for (final File file : allImageFiles) {
        try {
          db.gatherData(file.getAbsolutePath());
        } catch (final Exception ex) {
          System.err.println(file.toString() + " caused " + ex);
        }
      }
    }
  }
}
