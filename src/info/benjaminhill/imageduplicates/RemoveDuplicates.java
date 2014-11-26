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

import static info.benjaminhill.imageduplicates.CollectStats.*;
import info.benjaminhill.pcache.PCache;
import info.benjaminhill.pcache.PCacheEntry;
import info.benjaminhill.util.HashUtil;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Benjamin Hill
 */
public class RemoveDuplicates {

  private static final Logger LOG = Logger.getLogger(RemoveDuplicates.class.getName());

  public static void main(final String... args) throws IOException, Exception {
    logConfig();
    findDuplicates();
  }

  private static void findDuplicates() {
    try (final PCache<PCacheEntry> db = CollectStats.buildDB();) {

      final Map<String, PCacheEntry> all = db.getAll();
      System.out.println(String.format("Images found: %s", all.size()));

      LOG.log(Level.INFO, "Size: {0}", all.size());
      final SortedSet<String> goodKeys = new ConcurrentSkipListSet<>();

      for (final Map.Entry<String, PCacheEntry> ent : all.entrySet()) {
        if (ent.getValue().containsKey(IMG_HASH_FULL) && ent.getValue().containsKey(IMG_HASH_PERCEPT)
                && ent.getValue().getLong(IMAGE_HEIGHT) > 800 && ent.getValue().getLong(IMAGE_WIDTH) > 800) {
          goodKeys.add(ent.getKey());
        }
      }
      LOG.log(Level.INFO, "Images with perceptual hash and large dimensions: {0}", goodKeys.size());

      LOG.log(Level.INFO, "Exact matches");
      // Exact matches first, every pairwise comparison
      for (final String key1 : goodKeys) {
        final PCacheEntry ent1 = all.get(key1);
        for (final String key2 : goodKeys.subSet(goodKeys.first(), key1)) {
          final PCacheEntry ent2 = all.get(key2);

          if (Arrays.equals(ent1.getBytes(FILE_HASH), ent2.getBytes(FILE_HASH))) {
            System.out.println(String.format("FILE EXACT\t%s\t%s", ent1.getPk(), ent2.getPk()));
            if (!Objects.equals(ent1.getLong(FILE_LENGTH), ent2.getLong(FILE_LENGTH))) {
              throw new IllegalStateException("File hash matched, but length didn't?  Why do these files not match?");
            }
            continue;
          }

          if (Arrays.equals(ent1.getBytes(IMG_HASH_FULL), ent2.getBytes(IMG_HASH_FULL))) {
            System.out.println(String.format("IMAGE EXACT\t%s\t%s", ent1.getPk(), ent2.getPk()));
            if (!Objects.equals(ent1.getLong(IMAGE_WIDTH), ent2.getLong(IMAGE_WIDTH))) {
              LOG.severe(String.format("%s\t%s\t%s\t%s", ent1.getPk(), 
                      HashUtil.encodeToString(ent1.getBytes(IMG_HASH_FULL)), 
                      ent1.getLong(IMAGE_WIDTH), 
                      ent1.getLong(IMAGE_HEIGHT)));
              LOG.severe(String.format("%s\t%s\t%s\t%s", ent2.getPk(), 
                      HashUtil.encodeToString(ent2.getBytes(IMG_HASH_FULL)), 
                      ent2.getLong(IMAGE_WIDTH), 
                      ent2.getLong(IMAGE_HEIGHT)));
              throw new IllegalStateException("Image hash matched, but size didn't?  Why do these images not have same widths? ");
            }
            continue;
          }

          final int dist = HashUtil.hammingDistance(ent1.getBytes(IMG_HASH_PERCEPT), all.get(key2).getBytes(IMG_HASH_PERCEPT));
          if (dist < 1) {
            System.out.println(String.format("PERCEPT%s\t%s\t%s", dist, ent1.getPk(), ent2.getPk()));
          }
        }
      }

    }
  }
}
