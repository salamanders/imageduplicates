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

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author benjamin
 */
public class Main {

  public static void logConfig() {
    final Logger topLogger = java.util.logging.Logger.getLogger("");
    Handler consoleHandler = null;
    for (final Handler handler : topLogger.getHandlers()) {
      if (handler instanceof ConsoleHandler) {
        //found the console handler
        consoleHandler = handler;
        break;
      }
    }
    if (consoleHandler == null) {
      consoleHandler = new ConsoleHandler();
      topLogger.addHandler(consoleHandler);
    }
    consoleHandler.setLevel(java.util.logging.Level.FINEST);
  }

  public static void main(final String... args) throws IOException, Exception {
    logConfig();
    
    final JFileChooser jfc = new JFileChooser();
    jfc.setDialogTitle("Choose the folder to scan");
    jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    jfc.setApproveButtonText("Start in this Folder");
    final int returnVal = jfc.showOpenDialog(null);
    if (returnVal != JFileChooser.APPROVE_OPTION) {
      throw new RuntimeException("No directory chosen.");
    }
    // return Paths.get(jfc.getSelectedFile().toURI());
  }
}
