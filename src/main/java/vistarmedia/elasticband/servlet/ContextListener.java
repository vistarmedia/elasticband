package vistarmedia.elasticband.servlet;

import javax.servlet.ServletContextEvent;

import org.apache.log4j.Logger;

import vistarmedia.elasticband.runtime.ElasticBandRuntime;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class ContextListener extends GuiceServletContextListener {
  private Injector injector;
  private static final Logger log = Logger.getLogger(ContextListener.class);
  
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    super.contextInitialized(sce);
    log.info("Initializing context");
  }
  
  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    super.contextDestroyed(sce);
    log.info("Destorying Context");
    try {
      injector.getInstance(ElasticBandRuntime.class).destory();
    } catch(Throwable t) {
      log.fatal("Couldn't destroy runtime", t);
    }
  }
  
  @Override
  protected Injector getInjector() {
    log.info("Creating Injector");
    injector = Guice.createInjector(new ElasticBandConfig());
    return injector;
  }

}
