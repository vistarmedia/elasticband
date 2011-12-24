package vistarmedia.elasticband.runtime;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class ProcessLogger implements Runnable {
  private static Logger log = Logger.getLogger(ProcessLogger.class);

  private BufferedInputStream   stdout;
  private BufferedInputStream   stderr;
  private long          sleepMillis;
  private boolean       running;
  private StringBuffer  stdoutBuf;
  private StringBuffer  stderrBuf;

  public ProcessLogger(InputStream stdout, InputStream stderr) {
    this.stdout = new BufferedInputStream(stdout);
    this.stderr = new BufferedInputStream(stderr);
    this.sleepMillis = 500;
    this.running = true;
    this.stdoutBuf = new StringBuffer();
    this.stderrBuf = new StringBuffer();
  }

  public void destroy() {
    this.running = false;
  }

  public void run() {
    while (running) {
      try {
        boolean anyRead = false;
        
        if (stdout.available() > 0) {
          anyRead = true;
          char next = (char) stdout.read();
          if (next == '\n') {
            log.info(stdoutBuf.toString());
            stdoutBuf = new StringBuffer();
          } else {
            stdoutBuf.append(next);
          }
        }
        
        if (stderr.available() > 0) {
          anyRead = true;
          char next = (char) stderr.read();
          if (next == '\n') {
            log.error(stderrBuf.toString());
            stderrBuf = new StringBuffer();
          } else {
            stderrBuf.append(next);
          }
        }
        
        if(! anyRead) {
          Thread.sleep(sleepMillis);
        }
      } catch (Exception e) {
        log.error("Error reading input", e);
      }
    }
    log.info("Shutting down logger");
  }

}
