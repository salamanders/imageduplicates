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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility wrapper around whatever library seems best (right now Guava) to hash various types of data
 * @author benjamin
 */
public class BetterHash {

  private static final HashFunction HF = Hashing.goodFastHash(Long.SIZE);

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
   * @return Now using Guava hasher, better than before!
   */
  public static long hashFile(final File f) {
    try {
      return Files.hash(f, HF).padToLong();
    } catch (final IOException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  public static long hash(final byte[] input) {
    return HF.hashBytes(input).padToLong();
  }

  private BetterHash() {
    // empty
  }

}
