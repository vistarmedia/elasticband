package vistarmedia.elasticband.runtime;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;


public class WorkerProcess implements Runnable {

  private static final Logger log          = Logger
                                               .getLogger(WorkerProcess.class);
  private ProcessBuilder      builder;
  private Process             process;
  private ProcessLogger       logger;
  private AtomicInteger       loggerNumber = new AtomicInteger(0);

  public WorkerProcess(ProcessBuilder builder) {
    this.builder = builder;
  }

  public void destroy() {
    log.info("Destroying process");
    if (process != null) {
      process.destroy();
    } else {
      log.warn("Tried to kill a non-existant process");
    }
    if (logger != null) {
      logger.destroy();
    }
  }

  public void run() {
    try {
      log.info("Running command " + builder.command() + " from "
          + builder.directory());
      process = builder.start();
    } catch (Throwable t) {
      log.error("Couldn't start process", t);
    }

    if (process != null) {
      Integer exitValue = null;
      InputStream stdin = process.getInputStream();
      InputStream stderr = process.getErrorStream();
      logger = new ProcessLogger(stdin, stderr);
      getLoggerThread(logger).start();

      while (exitValue == null) {
        try {
          exitValue = process.waitFor();
        } catch (InterruptedException e) {
          log.info("Worker thread interrupted, continuing");
        }
      }
      log.info("Process exited with code: " + exitValue);
      if (logger != null) {
        logger.destroy();
      }
    }
  }

  private Thread getLoggerThread(Runnable runnable) {
    Thread thread = new Thread(runnable);
    thread.setName("WorkerLogger-" + loggerNumber.getAndIncrement());
    return thread;
  }

}
