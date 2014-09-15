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

package info.benjaminhill.pcache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Simple file-persisted cache You must provide a <code>
 * new CacheLoader<String, PCacheItem>() {
 * public PCacheItem load(final String key) { return build(key); } }
 * </code>
 *
 * @author benhill
 */
public class PCache<T extends Serializable> implements AutoCloseable {

  protected final String cacheFile;

  private final LoadingCache<String, T> lcache;

  public PCache(final CacheLoader<String, T> loader) {
    cacheFile = "cache." + this.getClass().getSimpleName() + ".gz";
    lcache = CacheBuilder.newBuilder().recordStats().build(loader);
    loadOldCache();
  }

  public PCache(final CacheLoader<String, T> loader, final String cacheName) {
    cacheFile = "cache." + cacheName + ".gz";
    lcache = CacheBuilder.newBuilder().recordStats().build(loader);
    loadOldCache();
  }

  @Override
  public void close() {
    save();
  }

  public T get(final String key) {
    return lcache.getUnchecked(key);
  }

  public ConcurrentMap<String, T> getAll() {
    return lcache.asMap();
  }

  private void loadOldCache() {
    final File oldCache = new File(cacheFile);
    if (oldCache.canRead()) {
      try (final ObjectInputStream oos =
          new ObjectInputStream(new GZIPInputStream(new FileInputStream(cacheFile)))) {
        this.lcache.putAll((Map) oos.readObject());
        System.err.println("Read cache file.");
      } catch (final Exception ex) {
        throw new IllegalArgumentException(ex);
      }
    }
  }

  public void save() {
    try (final ObjectOutputStream oos =
        new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(cacheFile)))) {
      final HashMap<String, T> tempMap = new HashMap<>();
      tempMap.putAll(this.lcache.asMap());
      oos.writeObject(tempMap);
    } catch (final Exception ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  // TODO: CacheLoader.loadAll
}
