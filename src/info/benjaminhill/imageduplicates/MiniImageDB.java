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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.imageio.ImageIO;

/**
 * Dead simple DB wrapper
 * TODO: update images set minrot = hex(min(rot0, rot1, rot2, rot3))
 * @author benjamin
 */
public class MiniImageDB implements AutoCloseable {

  final private Connection conn;
  private long calls = 0;

  final private PreparedStatement ensure;

  public MiniImageDB() {
    try {
      Class.forName("org.sqlite.JDBC");
      conn = DriverManager.getConnection("jdbc:sqlite:mydb.sqlite");

      try (final Statement stat = conn.createStatement()) {
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS `images` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL "
                + ", `file_hash` BLOB NOT NULL "
                + ", `file_length` NUMERIC NOT NULL "
                + ", `file_name` TEXT NOT NULL "
                + ", `file_path` TEXT NOT NULL "
                + ", `hash0` NUMERIC NOT NULL "
                + ", `hash90` NUMERIC NOT NULL "
                + ", `hash180` NUMERIC NOT NULL "
                + ", `hash270` NUMERIC NOT NULL "
                + ")");
      }
      ensure = conn.prepareStatement("INSERT into images (file_hash, file_length, file_name, file_path, hash0, hash90, hash180, hash270) VALUES (?,?,?,?,?,?,?,?)");
      conn.setAutoCommit(false);
    } catch (final ClassNotFoundException | SQLException ex) {
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

      ensure.setInt(5, imageHash0);
      ensure.setInt(6, imageHash90);
      ensure.setInt(7, imageHash180);
      ensure.setInt(8, imageHash270);
      ensure.execute();
      calls++;
    } catch (SQLException ex) {
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

  @Override
  public void close() {
    try {
      conn.setAutoCommit(true);
      conn.setAutoCommit(false);
      conn.close();
    } catch (final SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
}
