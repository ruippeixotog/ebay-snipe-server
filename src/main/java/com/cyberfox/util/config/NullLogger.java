package com.cyberfox.util.config;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
* User: Morgan
* Date: Dec 20, 2008
* Time: 3:17:26 PM
* To change this template use File | Settings | File Templates.
*/
class NullLogger implements LoggerInterface {
  public void logDebug(String foo) { System.out.println("DEBUG " + foo);}
  public void logMessage(String foo) { System.out.println("MSG " + foo);}
  public void handleException(String msg, Throwable e) { System.out.println("ERROR " + msg); e.printStackTrace(); }
  public void logVerboseDebug(String msg) { }
  public void handleDebugException(String sError, Throwable e) { }
  public void logFile(String msgtop, StringBuffer dumpsb) { }
  public void dump2File(String fname, StringBuffer sb) { }
  public void dumpFile(StringBuffer sb) { }

  public String getLog() { return "null"; }
  public File closeLog() { return null; }
  public boolean openLog(File fp) { return true; }

  public void pause() { }
  public void resume() { }

  public void addHandler(ErrorHandler eh) { }
}
