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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * Generic file utilities like hashing
 *
 * @author benjamin
 */
public class FileUtils {

  public static final Set<String> EXCLUDES = Sets.newHashSet("/.", "iphoto library", "temp",
      "library/developer", "library/application support/");

  private static final HashFunction HF = Hashing.goodFastHash(Long.SIZE);

  private static final Logger LOG = Logger.getLogger(FileUtils.class.getName());

  public static SortedSet<File> getFilesByExtensions(final String... extensions) throws IOException {
    final Set<String> extensionSet = Sets.newHashSet(extensions);
    final SortedSet<File> files = new ConcurrentSkipListSet<>();

    final Path root = Paths.get(System.getProperty("user.home"));

    java.nio.file.Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
        for (final String exclude : EXCLUDES) {
          if (dir.toString().toLowerCase().contains(exclude)) {
            LOG.log(Level.FINE, "Bailed on subdir:{0}", dir.toString());
            return FileVisitResult.SKIP_SUBTREE;
          }
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(final Path filePath, final BasicFileAttributes attrs) {
        if (extensionSet.contains(Files.getFileExtension(filePath.getFileName().toString()
            .toLowerCase()))) {
          files.add(filePath.toFile());
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(final Path file, final IOException e)
          throws IOException {
        System.err.printf("Visiting failed for %s%n", file);
        return FileVisitResult.SKIP_SUBTREE;
      }
    });

    return files;
  }

  public static long hash(final byte[] input) {
    return HF.hashBytes(input).padToLong();
  }

  public static long hash(final int[] input) {
    final ByteBuffer bb = ByteBuffer.allocate(input.length * 4);
    bb.asIntBuffer().put(input);
    return HF.hashBytes(bb.array()).padToLong();
  }

  public static long hash(final String input) {
    return HF.hashBytes(input.getBytes()).padToLong();
  }

  /**
   *
   * @param f
   * @return Thread-safe hash of a file. Reads entire file into memory in one pass (bad)
   */
  public static long hashFile(final File f) {
    try {
      return Files.hash(f, HF).padToLong();
    } catch (final IOException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  private FileUtils() {
    // empty
  }
}
