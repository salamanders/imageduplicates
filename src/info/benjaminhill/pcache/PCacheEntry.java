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
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Persistent Cacheable Item to save you some time if you need more than a default type. Mostly a
 * map. Should be extended to have convenience getter/setters
 *
 * @author benhill
 */
public class PCacheEntry implements Serializable {

  private static final long serialVersionUID = -7558236817030592787L;
  /**
   * Override if you want a PK of another name
   */
  protected String pkCol = "id";
  private final Map<String, Serializable> vals;

  public PCacheEntry() {
    vals = new HashMap<>();
  }

  public Map<String, Object> getAll() {
    return new ImmutableMap.Builder<String, Object>().putAll(vals).build();
  }

  public Double getDouble(final String col) {
    return (Double) vals.get(col);
  }

  public Long getLong(final String col) {
    final Number n = (Number) vals.get(col);
    assert n != null;

    if (n instanceof Long) {
      return (Long) n;
    }
    if (n instanceof Integer) {
      return new Long((Integer) n);
    }
    throw new java.lang.IllegalArgumentException("getLong ran into a non-Long or non-Integer:"
        + col + " " + n.getClass().getName());
  }

  public String getPk() {
    return getString(pkCol);
  }

  public String getString(final String col) {
    return vals.get(col).toString();
  }

  public void put(final String col, final Serializable val) {
    vals.put(col, val);
  }

  public void setPk(final String val) {
    put(pkCol, val);
  }
}
