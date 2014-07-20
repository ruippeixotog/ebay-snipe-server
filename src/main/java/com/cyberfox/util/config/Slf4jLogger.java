package com.cyberfox.util.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

class Slf4jLogger implements LoggerInterface {
  private Logger log = LoggerFactory.getLogger("JConfig");

  public void logDebug(String foo) { log.debug(foo); }
  public void logMessage(String foo) { log.info(foo); }
  public void handleException(String msg, Throwable e) { log.error(msg, e); }
  public void logVerboseDebug(String msg) { log.trace(msg); }
  public void handleDebugException(String sError, Throwable e) { log.error(sError, e); }
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
