package info.benjaminhill.imageduplicates;

import java.util.Set;

/**
 * @author benhill
 *
 */
public class IdentifyDuplicates {

  public static void main(final String... args) {
    try (final MiniImageDB db = new MiniImageDB()) {
      final Set<Set<String>> matches = db.getClusters();
      System.out.printf("Found %s clusters.%n", matches.size());
      int groupNum = 0;
      for (final Set<String> matchGroup : matches) {
        groupNum++;
        for (final String path : matchGroup) {
          System.out.printf("%s\t%s%n", groupNum, path);
        }
      }
    }
  }
}
