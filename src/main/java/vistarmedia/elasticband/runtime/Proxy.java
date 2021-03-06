package vistarmedia.elasticband.runtime;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.inject.Singleton;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;

@Singleton
public class Proxy {
  private int                 applicationPort;
  private static final long   maxWaitMillis = 1000;
  private static final Logger log           = Logger.getLogger(Proxy.class);
  private AsyncHttpClient     client;
  private ElasticBandRuntime  runtime;

  @Inject
  public Proxy(ElasticBandRuntime runtime,
      @Named("application.port") int applicationPort) {
    this.applicationPort = applicationPort;
    this.runtime = runtime;
    
    AsyncHttpClientConfig config = new Builder().build();
    this.client = new AsyncHttpClient(config);
  }
  
  public void doGet(HttpServletRequest req, HttpServletResponse res) {
    runtime.ensureRunning();
    String url = prepareUrl(req);
    handleSafely(client.prepareGet(url), req, res, false);
  }
  
  public void doPost(HttpServletRequest req, HttpServletResponse res) {
    runtime.ensureRunning();
    String url = prepareUrl(req);
    handleSafely(client.preparePost(url), req, res, true);
  }
  
  public void doHead(HttpServletRequest req, HttpServletResponse res) {
    runtime.ensureRunning();
    String url = prepareUrl(req);
    handleSafely(client.prepareHead(url), req, res, false);
  }
  
  public void doOptions(HttpServletRequest req, HttpServletResponse res) {
    runtime.ensureRunning();
    String url = prepareUrl(req);
    handleSafely(client.prepareOptions(url), req, res, false);
  }
  
  public void doPut(HttpServletRequest req, HttpServletResponse res) {
    runtime.ensureRunning();
    String url = prepareUrl(req);
    handleSafely(client.preparePut(url), req, res, true);
  }
  
  public void doDelete(HttpServletRequest req, HttpServletResponse res) {
    runtime.ensureRunning();
    String url = prepareUrl(req);
    handleSafely(client.prepareDelete(url), req, res, false);
  }
  
  private void handleSafely(BoundRequestBuilder conn, HttpServletRequest req,
      HttpServletResponse res, boolean writeBody) {
    try {
      handleRequest(conn, req, res, writeBody);
    } catch (Throwable t) {
      log.error("Request Error", t);
      try {
        PrintWriter out = res.getWriter();
        res.sendError(500, t.getLocalizedMessage());
        t.printStackTrace(out);
      } catch (IOException e) {
        res.setStatus(500);
        log.error("Broken Request", e);
      }
    }
  }

  private void handleRequest(BoundRequestBuilder conn,
      final HttpServletRequest req, final HttpServletResponse res,
      boolean writeBody) throws IOException, InterruptedException {

    conn = copyHeaders(conn, req);
    if (writeBody) {
      conn = conn.setBody(req.getInputStream());
    }

    ProxyResponseHandler handler = new ProxyResponseHandler(req, res);
    conn.execute(handler);

    if(! handler.wait(maxWaitMillis, TimeUnit.MILLISECONDS)) {
      log.error("Request exceeded maximum time");
    }
  }

  private BoundRequestBuilder copyHeaders(BoundRequestBuilder conn,
      HttpServletRequest req) {
    Enumeration<String> headerNames = req.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String name = headerNames.nextElement();
      Enumeration<String> headerValues = req.getHeaders(name);
      while (headerValues.hasMoreElements()) {
        String value = headerValues.nextElement();
        conn = conn.addHeader(name, value);
      }
    }

    String source = req.getRemoteAddr();
    return conn.addHeader("X-Forwarded-For", source);
  }

  private String prepareUrl(HttpServletRequest req) {
    StringBuffer url = new StringBuffer("http://127.0.0.1:");
    url.append(applicationPort);
    url.append(req.getRequestURI());

    String queryString = req.getQueryString();
    if (queryString != null) {
      url.append('?');
      url.append(queryString);
    }
    return url.toString();
  }
}
