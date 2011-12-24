package vistarmedia.elasticband.runtime;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;


public abstract class ElasticBandRuntime {

  protected abstract ProcessBuilder getBuilder(String command);

  private static final Logger log = Logger.getLogger(ElasticBandRuntime.class);
  private Thread              workerThread;
  private WorkerProcess       process;
  private String              command;
  private AtomicInteger       workerCount = new AtomicInteger(0);

  /**
   * Optionally overridable method that will be called before an application is
   * launched for the first time. If any exceptions will be thrown, they should
   * be runtime so that the injection stack falls apart.
   */
  protected void prepare() {

  }

  public void ensureRunning() {
    if (workerThread == null || !workerThread.isAlive()) {
      // @TODO: Not thread safe
      log.info("Starting worker thread");
      ProcessBuilder builder = getBuilder(command);
      
      process = new WorkerProcess(builder);
      workerThread = getWorkerThread(process);
      workerThread.start();
    }
  }

  protected File getWorkingDirectory(File applicationRoot) {
    File webinfDir = new File(applicationRoot, "WEB-INF");
    return new File(webinfDir, "classes");
  }
  
  public void destory() throws InterruptedException {
    log.info("Cleaningly shutting down runtime");
    if(process != null) {
      process.destroy();
      if(workerThread != null) {
        workerThread.join();
      }
    }
  }

  @Inject
  public void setCommandFormat(
      @Named("application.command") String commandFormat,
      @Named("application.port") int port) {
    command = String.format(commandFormat, port);
  }
  
  private Thread getWorkerThread(Runnable runnable) {
    Thread thread = new Thread(runnable);
    thread.setName(String.format("Worker-%d", workerCount.getAndIncrement()));
    return thread;
  }
}
