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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Generic file utilities like hashing
 *
 * @author benjamin
 */
public class FileUtils {

  private static final ThreadLocal<MessageDigest> MD_SHA256 = new ThreadLocal<MessageDigest>() {
    @Override
    protected MessageDigest initialValue() {
      try {
        return MessageDigest.getInstance("SHA-256");
      } catch (final NoSuchAlgorithmException ex) {
        throw new RuntimeException(ex);
      }
    }
  };

  /**
   * For hashing image data
   *
   * @param data
   * @return
   */
  public static byte[] hashData(final byte[] data) {
    MD_SHA256.get().reset();
    return MD_SHA256.get().digest(data);
  }

  public static int hashData(final int[] data) {
    return Arrays.hashCode(data);
  }

  /**
   *
   * @param f
   * @return Thread-safe hash of a file
   */
  public static byte[] hashFile(final File f) {
    final long length = f.length();
    final byte[] msg = new byte[(int) length];
    try (final FileInputStream fis = new FileInputStream(f)) {
      if (fis.read(msg) != length) {
        throw new IOException("Less than full file read:" + f.toString());
      }
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    }

    MD_SHA256.get().reset();
    return MD_SHA256.get().digest(msg);
  }

  private FileUtils() {
    // empty
  }
}
