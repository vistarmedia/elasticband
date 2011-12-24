package vistarmedia.elasticband.os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import org.apache.log4j.Logger;

import vistarmedia.elasticband.runtime.ProcessLogger;

import com.google.inject.Singleton;

@Singleton
public class Shell {

  private static final Logger log = Logger.getLogger(Shell.class);
  
  public boolean download(URL from, File to) throws IOException {
    ReadableByteChannel dlchannel = Channels.newChannel(from.openStream());
    FileOutputStream out = new FileOutputStream(to);
    out.getChannel().transferFrom(dlchannel, 0, 1 << 24);
    return true;
  }
  
  public int execBlocking(List<String> command) throws IOException {
    log.info("Running: "+ command);
    
    Integer exitCode = null;
    ProcessBuilder builder = new ProcessBuilder(command);
    Process proc = builder.start();
    
    InputStream stdout = proc.getInputStream();
    InputStream stderr = proc.getErrorStream();
    ProcessLogger logger = new ProcessLogger(stdout, stderr);
    Thread loggerThread = getLoggerThread(logger);
    loggerThread.start();
    
    while(exitCode == null) {
      try {
        exitCode = proc.waitFor();
      } catch (InterruptedException e) {
        log.info("process interrupted, continuing");
      }
    }
    log.info("Process exited with "+ exitCode);
    logger.destroy();
    log.info("Waiting for logger thread to exit...");
    try {
      loggerThread.join();
    } catch (InterruptedException e) {
      log.error("Interrupted waiting on logger", e); 
    }
    return exitCode;
  }
  
  private Thread getLoggerThread(Runnable runnable) {
    Thread thread = new Thread(runnable);
    thread.setName("Worker Logger");
    thread.setDaemon(true);
    return thread;
  }
}
