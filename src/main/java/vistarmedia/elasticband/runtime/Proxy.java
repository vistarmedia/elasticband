package vistarmedia.elasticband.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.inject.Singleton;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig.Builder;

@Singleton
public class Proxy {
  private Semaphore           connLock;
  private int                 applicationPort;
  private static final long   maxWaitMillis = 1000;
  private static final Logger log           = Logger.getLogger(Proxy.class);
  private AsyncHttpClient     client;
  private ElasticBandRuntime  runtime;

  @Inject
  public Proxy(ElasticBandRuntime runtime,
      @Named("maxconnections") int maxConnections,
      @Named("application.port") int applicationPort) {
    this.connLock = new Semaphore(maxConnections);
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

    final CountDownLatch latch = new CountDownLatch(1);
    final InputStream body = req.getInputStream();
    final OutputStream responseBody = res.getOutputStream();

    conn = copyHeaders(conn, req);
    if (writeBody) {
      conn = conn.setBody(body);
    }

    connLock.acquire();
    conn.execute(new AsyncHandler<Void>() {

      public STATE onBodyPartReceived(HttpResponseBodyPart part)
          throws Exception {
        responseBody.write(part.getBodyPartBytes());
        return STATE.CONTINUE;
      }

      public Void onCompleted() throws Exception {
        responseBody.close();
        latch.countDown();
        return null;
      }

      public STATE onHeadersReceived(HttpResponseHeaders headers)
          throws Exception {
        for (Entry<String, List<String>> entry : headers.getHeaders()) {
          String name = entry.getKey();
          for (String value : entry.getValue()) {
            res.addHeader(name, value);
          }
        }
        return STATE.CONTINUE;
      }

      public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
        int code = status.getStatusCode();
        res.setStatus(code);
        if (code != 200) {
          res.sendError(code, status.getStatusText());
        } else {
          res.setStatus(HttpServletResponse.SC_OK);
        }
        return STATE.CONTINUE;
      }

      public void onThrowable(Throwable t) {
        log.error("Response Error", t);
        try {
          res.sendError(500, t.getLocalizedMessage());
          latch.countDown();
        } catch (IOException e) {
          log.error("Uncatchable response error", e);
        }
      }

    });

    if (!latch.await(maxWaitMillis, TimeUnit.MILLISECONDS)) {
      log.error("Request exceeded maximum time");
    }
    connLock.release();
  }

  @SuppressWarnings("unchecked")
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
