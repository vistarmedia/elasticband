package vistarmedia.elasticband.servlet;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import vistarmedia.elasticband.runtime.ElasticBandRuntime;

import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

public class ElasticBandConfig extends ServletModule {
  private static final Logger log               = Logger
                                                    .getLogger(ElasticBandConfig.class);

  private static final int    MAX_CONNECTIONS   = 10;
  private static final String VENV_FALLBACK_URL = "https://raw.github.com/pypa/virtualenv/master/virtualenv.py";

  // @TODO: static directories
  @Override
  protected void configureServlets() {
    Properties config = null;
    Class<ElasticBandRuntime> runtimeClass = null;

    try {
      config = getContextProperties();
      runtimeClass = getRuntime(config.get("application.runtime").toString());
    } catch (Throwable t) {
      log.fatal("Couldn't create runtime", t);
      throw new RuntimeException(t);
    }

    Names.bindProperties(binder(), config);
    bind(ElasticBandRuntime.class).to(runtimeClass);
    serve("*").with(Servlet.class);
  }

  private Class<ElasticBandRuntime> getRuntime(String name)
      throws ClassNotFoundException {
    if ("python".equals(name) || name == null) {
      name = "vistarmedia.elasticband.runtime.python.PythonRuntime";
    }

    if ("virtualenv".equals(name)) {
      name = "vistarmedia.elasticband.runtime.python.VirtualenvRuntime";
    }

    log.info("Delcared Runtime: " + name);
    @SuppressWarnings("unchecked")
    Class<ElasticBandRuntime> rtClass = (Class<ElasticBandRuntime>) Class
        .forName(name);
    return rtClass;
  }

  private Properties getContextProperties() {
    ServletContext ctx = getServletContext();
    Properties properties = new Properties();

    @SuppressWarnings("unchecked")
    Enumeration<String> propNames = ctx.getInitParameterNames();
    while (propNames.hasMoreElements()) {
      String key = propNames.nextElement();
      Object value = ctx.getInitParameter(key);
      log.info(key + " bound to " + value);
      if (key != null && value != null) {
        properties.put(key, value);
      }
    }

    properties.put("application.root", ctx.getRealPath("/"));
    properties.put("application.port", "" + getFreePort());
    properties.put("maxconnections", "" + MAX_CONNECTIONS);
    properties.put("python.virtualenv.url", VENV_FALLBACK_URL);
    return properties;
  }

  private int getFreePort() {
    int port = 60551;
    ServerSocket socket = null;

    try {
      socket = new ServerSocket(0);
      port = socket.getLocalPort();
      socket.close();
    } catch (IOException e) {
      log.error("Problem getting free port, falling back to 60551", e);
    }
    return port;
  }
}
