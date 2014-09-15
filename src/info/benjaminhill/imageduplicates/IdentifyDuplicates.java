package info.benjaminhill.imageduplicates;

import static info.benjaminhill.imageduplicates.ReadImageMetadata.buildDB;
import info.benjaminhill.pcache.PCache;
import info.benjaminhill.pcache.PCacheEntry;

import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author benhill
 *
 */
public class IdentifyDuplicates {

  private static final Logger LOG = Logger.getAnonymousLogger();

  public static void main(final String... args) {

    final Handler consoleHandler = new ConsoleHandler();
    consoleHandler.setLevel(Level.FINER);
    LOG.addHandler(consoleHandler);
    try (final PCache<PCacheEntry> db = buildDB();) {
      final Map<String, PCacheEntry> all = db.getAll();
      System.out.println(all.size());
      System.out.println();

      for (final PCacheEntry ent : all.values()) {
        if (ent.getLong("h") < 50 || ent.getLong("w") < 50) {
          continue;
        }
        System.out.print(ent.getPk() + "\t" + ent.getDouble("r") + "\t");
        int minDist = Integer.MAX_VALUE;
        PCacheEntry minOther = null;
        for (final PCacheEntry other : all.values()) {
          if (other.getLong("h") < 50 || other.getLong("w") < 50) {
            continue;
          }
          if (ent.getPk().equals(other.getPk())) {
            continue;
          }
          // Not the same image ratio
          if (Math.round(100 * (ent.getDouble("r") - other.getDouble("r"))) > 0) {
            continue;
          }
          final int newDist =
              ImageFingerprint.hammingBinaryDistance(ent.getLong("imgfp_2"),
                  other.getLong("imgfp_2"));
          if (newDist < minDist) {
            minDist = newDist;
            minOther = other;
          }
        }
        if (minOther != null) {
          System.out.println(minOther.getPk() + "\t" + minDist + "\t" + minOther.getDouble("r"));

        }
      }
    }
  }
  /*
   * throws IOException, Exception {
   *
   * try (final PCache<PCacheEntry> db = buildDB();) {
   *
   * final Set<Set<String>> matches = db.getClusters(); System.out.printf("Found %s clusters.%n",
   * matches.size()); int groupNum = 0; for (final Set<String> matchGroup : matches) { groupNum++;
   * for (final String path : matchGroup) { System.out.printf("%s\t%s%n", groupNum, path); } }
   *
   * } }
   *
   *
   *
   * public Set<Set<String>> getClusters() { final Map<Integer, Set<Integer>> id2group = new
   * ConcurrentHashMap<>(2 << 16); final Map<Integer, String> id2path = new ConcurrentHashMap<>(2 <<
   * 16);
   *
   * try (final Statement stmt = conn.createStatement(); final ResultSet rs =
   * stmt.executeQuery("select i1.id, i1.file_path fp1, i1.file_name fn1" + " from images i1");) {
   * long row = 0; while (rs.next()) { row++; if (row % 5_000 == 0) {
   * System.out.println("Preloading row:" + row); } final String f1 = rs.getString("fp1") +
   * File.separator + rs.getString("fn1"); final int f1id = rs.getInt("id"); id2path.put(f1id, f1);
   *
   * final Set<Integer> newSet = new HashSet<>(); newSet.add(f1id); id2group.put(f1id, newSet); } }
   * catch (final SQLException ex) { throw new RuntimeException(ex); }
   *
   * System.out.println("Done preloading.");
   *
   * try (final Statement stmt = conn.createStatement(); final ResultSet rs =
   * stmt.executeQuery("select i1.id id1" + ", i2.id id2" + " FROM images i1, images i2" +
   * " WHERE i1.id<i2.id AND i1.file_length>10000 AND (" + " i1.hash_min=i2.hash_min " +
   * " OR i1.file_hash=i2.file_hash" + ")")) { long row = 0; while (rs.next()) { if (row % 5_000 ==
   * 0) { System.out.println("Reading combo row:" + row); } final int f1id = rs.getInt("id1"); final
   * int f2id = rs.getInt("id2");
   *
   * // assert file2group.containsKey(f1id) && file2group.containsKey(f2id); // If they aren't the
   * same, make it so. if (id2group.get(f1id) != id2group.get(f2id)) {
   * id2group.get(f1id).addAll(id2group.remove(f2id)); id2group.put(f2id, id2group.get(f1id)); }
   * row++; } } catch (final SQLException ex) { throw new RuntimeException(ex); }
   *
   * final HashSet<Set<String>> result = new HashSet<>(); for (final Set<Integer> potential :
   * id2group.values()) { if (potential.size() > 1) { final HashSet<String> resultSet = new
   * HashSet<>(); for (final Integer id : potential) { resultSet.add(id2path.get(id)); }
   * result.add(resultSet); } } return result; }
   */
}
