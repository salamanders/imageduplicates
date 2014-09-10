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
package info.benjaminhill.imageduplicates;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.google.common.primitives.Ints;

/**
 * Dead simple DB wrapper TODO: update images set minrot = hex(min(rot0, rot1, rot2, rot3))
 *
 * @author benjamin
 */
public class MiniImageDB implements AutoCloseable {

  private long calls = 0;
  final private Connection conn;

  final private PreparedStatement ensure;

  public MiniImageDB() {
    try {
      Class.forName("org.sqlite.JDBC");
      conn = DriverManager.getConnection("jdbc:sqlite:mydb.sqlite");

      try (final Statement stat = conn.createStatement();) {
        stat.executeUpdate("PRAGMA synchronous = OFF;");
        stat.executeUpdate("PRAGMA journal_mode = MEMORY;");
        stat.executeUpdate("PRAGMA cache_size = 20000;");// 10x
      }

      try (final Statement stat = conn.createStatement()) {
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS `images` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL "
            + ", `file_hash` BLOB NOT NULL "
            + ", `file_length` NUMERIC NOT NULL "
            + ", `file_name` TEXT NOT NULL "
            + ", `file_path` TEXT NOT NULL "
            + ", `hash_min` INTEGER NOT NULL " + ")");
      }
      ensure =
          conn.prepareStatement("INSERT into images"
          + " (file_hash, file_length, file_name, file_path, hash_min)" + " VALUES (?,?,?,?,?)");
      conn.setAutoCommit(false);
    } catch (final ClassNotFoundException | SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void close() {
    try {
      conn.setAutoCommit(true);
      try (final Statement stat = conn.createStatement();) {
        stat.executeUpdate("VACUUM;");
      }
      conn.close();
    } catch (final SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void gatherData(final String filePath) throws IOException {
    try {
      final File f = new File(filePath);
      if (!f.isFile() || !f.canRead()) {
        throw new IllegalArgumentException("Unable to read:" + filePath);
      }

      ensure.clearParameters();
      ensure.setBytes(1, FileUtils.hashFile(f));
      ensure.setLong(2, f.length());
      ensure.setString(3, f.getName());
      ensure.setString(4, f.getParent());

      BufferedImage thumb = ImageUtils.getScaledInstance(ImageIO.read(f), 64);

      final int imageHash0 = FileUtils.hashData(ImageUtils.getData(thumb));
      thumb = ImageUtils.rotate90(thumb);
      final int imageHash90 = FileUtils.hashData(ImageUtils.getData(thumb));
      thumb = ImageUtils.rotate90(thumb);
      final int imageHash180 = FileUtils.hashData(ImageUtils.getData(thumb));
      thumb = ImageUtils.rotate90(thumb);
      final int imageHash270 = FileUtils.hashData(ImageUtils.getData(thumb));

      ensure.setInt(5, Ints.min(imageHash0, imageHash90, imageHash180, imageHash270));
      ensure.execute();
      calls++;
    } catch (final SQLException ex) {
      throw new RuntimeException(ex);
    }
    if (calls % 500 == 0) {
      try {
        conn.setAutoCommit(true);
        conn.setAutoCommit(false);
      } catch (final SQLException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  /**
   * Creates clusters of matching images. There is likely a better way to do it than all this set
   * merging, but meh.
   *
   * @return
   */
  public Set<Set<String>> getClusters() {
    final Map<Integer, Set<Integer>> id2group = new ConcurrentHashMap<>(2 << 16);
    final Map<Integer, String> id2path = new ConcurrentHashMap<>(2 << 16);

    try (final Statement stmt = conn.createStatement();
        final ResultSet rs =
            stmt.executeQuery("select i1.id, i1.file_path fp1, i1.file_name fn1"
                + " from images i1");) {
      long row = 0;
      while (rs.next()) {
        row++;
        if (row % 10_000 == 0) {
          System.out.println("Preloading row:" + row);
        }
        final String f1 = rs.getString("fp1") + File.separator + rs.getString("fn1");
        final int f1id = rs.getInt("id");
        id2path.put(f1id, f1);

        final Set<Integer> newSet = new HashSet<>();
        newSet.add(f1id);
        id2group.put(f1id, newSet);
      }
    } catch (final SQLException ex) {
      throw new RuntimeException(ex);
    }

    try (final Statement stmt = conn.createStatement();
        final ResultSet rs =
            stmt.executeQuery("select i1.id id1" + ", i2.id id2" + " FROM images i1, images i2"
                + " WHERE i1.id<i2.id AND i1.file_length>10000 AND (" + " i1.hash_min=i2.hash_min "
                + " OR i1.file_hash=i2.file_hash" + ")")) {
      long row = 0;
      while (rs.next()) {
        row++;
        if (row % 10_000 == 0) {
          System.out.println("Reading combo row:" + row);
        }
        final int f1id = rs.getInt("id1");
        final int f2id = rs.getInt("id2");

        // assert file2group.containsKey(f1id) && file2group.containsKey(f2id);
        // If they aren't the same, make it so.
        if (id2group.get(f1id) != id2group.get(f2id)) {
          id2group.get(f1id).addAll(id2group.remove(f2id));
          id2group.put(f2id, id2group.get(f1id));
        }
      }
    } catch (final SQLException ex) {
      throw new RuntimeException(ex);
    }

    final HashSet<Set<String>> result = new HashSet<>();
    for (final Set<Integer> potential : id2group.values()) {
      if (potential.size() > 1) {
        final HashSet<String> resultSet = new HashSet<>();
        for (final Integer id : potential) {
          resultSet.add(id2path.get(id));
        }
        result.add(resultSet);
      }
    }
    return result;
  }
}
