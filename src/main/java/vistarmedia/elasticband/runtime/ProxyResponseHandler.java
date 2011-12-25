package vistarmedia.elasticband.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

public class ProxyResponseHandler implements AsyncHandler<Void> {
  private static final Logger       log   = Logger
                                              .getLogger(ProxyResponseHandler.class);
//  private final HttpServletRequest  req;
  private final HttpServletResponse res;
  private final OutputStream        responseBody;
  private final CountDownLatch      latch = new CountDownLatch(1);

  public ProxyResponseHandler(HttpServletRequest req, HttpServletResponse res) {
//    this.req = req;
    this.res = res;
    
    try {
      responseBody = res.getOutputStream();
    } catch (IOException e) {
      throw new RuntimeException("Couldn't get output", e);
    }
  }

  public boolean wait(Long time, TimeUnit unit) {
    while (true) {
      try {
        return latch.await(time, unit);
      } catch (InterruptedException e) {
        log.info("Interrupted waiting on response, retrying");
      }
    }
  }

  public void onThrowable(Throwable t) {
    log.error("Response Error", t);
    try {
      res.sendError(500, t.getLocalizedMessage());
    } catch(IOException e) {
      res.setStatus(500);
    }
  }

  public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart)
      throws Exception {
    responseBody.write(bodyPart.getBodyPartBytes());
    return STATE.CONTINUE;
  }

  public STATE onStatusReceived(HttpResponseStatus responseStatus)
      throws Exception {
    res.setStatus(responseStatus.getStatusCode());
    return STATE.CONTINUE;
  }

  public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
    for (Entry<String, List<String>> entry : headers.getHeaders()) {
      String name = entry.getKey();
      for (String value : entry.getValue()) {
        res.addHeader(name, value);
      }
    }
    return STATE.CONTINUE;
  }

  public Void onCompleted() throws Exception {
    responseBody.close();
    latch.countDown();
    return null;
  }
}
