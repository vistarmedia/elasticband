package vistarmedia.elasticband.servlet;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import vistarmedia.elasticband.runtime.Proxy;

@Singleton
public class Servlet extends HttpServlet {
  private static final long   serialVersionUID = 810323553509821938L;

  private Proxy proxy;

  @Inject
  public Servlet(Proxy proxy) {
    this.proxy = proxy;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) {
    this.proxy.doGet(req, res);
  }
  
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) {
    this.proxy.doPost(req, res);
  }
  
  @Override
  public void doHead(HttpServletRequest req, HttpServletResponse res) {
    this.proxy.doHead(req, res);
  }
  
  @Override
  public void doOptions(HttpServletRequest req, HttpServletResponse res) {
    this.proxy.doOptions(req, res);
  }
  
  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse res) {
    this.proxy.doPut(req, res);
  }
  
  
  @Override
  public void doDelete(HttpServletRequest req, HttpServletResponse res) {
    this.proxy.doDelete(req, res);
  }
}
