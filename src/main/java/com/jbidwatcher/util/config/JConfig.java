package com.jbidwatcher.util.config;

import com.jbidwatcher.util.Constants;

import java.io.File;

/**
 * User: mrs
 * Date: 6/9/11
 * Time: 3:48 PM
 *
 * JBidwatcher-specific configuration tools, with all the power of the general-purpose config class behind it.
 */
public class JConfig extends com.cyberfox.util.config.JConfig {

  static {
    setBaseName("JBidWatch.cfg");

    String version = null;
    Package pack = Constants.class.getPackage();
    if (pack != null) version = pack.getImplementationVersion();
    if (version == null) {
      version = "debug";
    }
  }

  public static boolean sendMetricsAllowed() {
    return queryConfiguration("metrics.optin", "false").equals("true") ||
           (queryConfiguration("metrics.optin", "false").equals("pre") && isPrerelease());
  }

  public static boolean isPrerelease() {return Constants.PROGRAM_VERS.matches(".*(pre|alpha|beta).*");}

  public static void stopMetrics() {
  }

  public static void fixupPaths(String homeDirectory) {
    String[][] s = {{"auctions.savepath", "auctionsave"},
        {"platform.path", "platform"},
        {"savefile", "auctions.xml"},
        {"search.savefile", "searches.xml"}};
    String sep = System.getProperty("file.separator");
    for (String[] pair : s) {
      setConfiguration(pair[0], homeDirectory + sep + pair[1]);
    }
  }

  public static String getVersion() {
    return Constants.PROGRAM_VERS;
  }

  public static File getContentFile(String identifier) {
    File fp = null;
    String outPath = queryConfiguration("auctions.savepath");
    if(outPath != null && outPath.length() != 0) {
      String filePath = outPath + System.getProperty("file.separator") + identifier + ".html.gz";
      fp = new File(filePath);
    }
    return fp;
  }
}
