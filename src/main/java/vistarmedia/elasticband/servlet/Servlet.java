package vistarmedia.elasticband.servlet;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import vistarmedia.elasticband.runtime.Proxy;

@Singleton
public class Servlet extends HttpServlet {
  private static final long   serialVersionUID = 810323553509821938L;
  private static final Logger log              = Logger
                                                   .getLogger(Servlet.class);

  private Proxy proxy;

  @Inject
  public Servlet(Proxy proxy) {
    this.proxy = proxy;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) {
    this.proxy.doGet(req, res);
  }
}
