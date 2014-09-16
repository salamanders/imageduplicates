package info.benjaminhill.imageduplicates;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
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

      for (final PCacheEntry ent1 : all.values()) {
        if(!ent1.containsKey("h")) {
          continue;
        }
        
        if (ent1.getLong("h") < 50 || ent1.getLong("w") < 50) {
          continue;
        }
        
        int minDist = Integer.MAX_VALUE;
        PCacheEntry minOther = null;
        
        for (final PCacheEntry ent2 : all.values()) {
          if(!ent2.containsKey("h")) {
            continue;
          }
          
          if (ent1.getPk().compareTo(ent2.getPk()) >= 0) {
            continue;
          }
          if (ent2.getLong("h") < 50 || ent2.getLong("w") < 50) {
            continue;
          }
          
          /*
          // Too effective for now!
          if (Math.round(100 * (ent1.getDouble("r") - ent2.getDouble("r"))) > 0) {
            continue;
          }
                  */

          final int newDist = 
                  1_000_000 * ImageFingerprint.hammingBinaryDistance(ent1.getLong("imgfp_0"), ent2.getLong("imgfp_0"))
                  + 10_000 * ImageFingerprint.hammingBinaryDistance(ent1.getLong("imgfp_1"), ent2.getLong("imgfp_1"))
                  + 100 * ImageFingerprint.hammingBinaryDistance(ent1.getLong("imgfp_2"), ent2.getLong("imgfp_2"))
                  //+ 10 * ImageFingerprint.hammingBinaryDistance(ent1.getLong("imgfp_3"), ent2.getLong("imgfp_3"))
                  ;

          if(newDist < minDist) {
            minOther = ent2;
            minDist = newDist;
          }
        }
        
        System.out.print(ent1.getPk());
        if(minOther!=null) {
          System.out.print("\t"+ minOther.getPk() + "\t" + minDist); 
        }
        System.out.println();
      }

      
    }
  }
}
