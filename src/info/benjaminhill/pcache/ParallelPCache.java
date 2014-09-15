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

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheLoader;

/**
 * Parallel execution version of the PCache. Call getFuture instead, and then blockForFinish
 *
 * @author benhill
 */
public class ParallelPCache<T extends Serializable> extends PCache {

  private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime()
      .availableProcessors());

  public ParallelPCache(final CacheLoader loader) {
    super(loader);
  }

  public ParallelPCache(final CacheLoader loader, final String cacheName) {
    super(loader, cacheName);
  }

  public void blockForFinish() {
    try {
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.DAYS);
    } catch (final InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Future<T> getFuture(final String key) {
    return executor.submit(new Callable<T>() {
      @Override
      public T call() {
        return (T) get(key);
      }
    });
  }

}
