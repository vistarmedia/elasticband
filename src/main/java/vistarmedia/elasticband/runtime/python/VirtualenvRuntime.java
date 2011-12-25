package vistarmedia.elasticband.runtime.python;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import vistarmedia.elasticband.os.Path;
import vistarmedia.elasticband.os.Shell;
import vistarmedia.elasticband.runtime.ElasticBandRuntime;

import com.google.inject.Singleton;

@Singleton
public class VirtualenvRuntime extends ElasticBandRuntime {

  private static final Logger log = Logger.getLogger(VirtualenvRuntime.class);

  private File                hostPython;
  private File                applicationRoot;
  private File                virtualenvRoot;
  private Shell               shell;
  private Path                path;
  private URL                 fallbackUrl;
  private File                python;
  private File                pip;

  @Inject
  public VirtualenvRuntime(Shell shell, Path path,
      @Named("python.virtualenv.url") String fallbackUrl,
      @Named("python.virtualenv.root") String root,
      @Named("application.root") String applicationRoot,
      @Named("application.requirements") String requirements) {
    try {
      this.fallbackUrl = new URL(fallbackUrl);
    } catch (MalformedURLException e) {
      bail("Invalid fallback URL", e);
    }
    this.applicationRoot = new File(applicationRoot);
    this.shell = shell;
    this.path = path;
    this.hostPython = getHostPython();
    this.virtualenvRoot = getRoot(root);

    createVirtualenv(this.virtualenvRoot);

    File binDir = new File(virtualenvRoot, "bin");
    this.python = new File(binDir, "python");
    this.pip = new File(binDir, "pip");

    installRequirements(requirements);
  }

  @Override
  protected ProcessBuilder getBuilder(String command) {
    List<String> cmd = new ArrayList<String>();
    cmd.add(python.getAbsolutePath());
    Collections.addAll(cmd, command.split("\\s+"));

    ProcessBuilder builder = new ProcessBuilder(cmd);
    builder.directory(getWorkingDirectory(applicationRoot));
    return builder;
  }

  private void installRequirements(String reqFileName) {
    File requirements = new File(getWorkingDirectory(applicationRoot),
        reqFileName);

    List<String> command = new ArrayList<String>();
    command.add(this.pip.getAbsolutePath());
    command.add("install");
    command.add("-r");
    command.add(requirements.getAbsolutePath());
    try {
      if (shell.execBlocking(command) != 0) {
        bail("Command returned non-zero exit code");
      }
    } catch (IOException e) {
      bail("Couldn't create virtualenv", e);
    }
  }

  private void createVirtualenv(File root) {
    File virtualenv = getVirtualenvExecutable();
    List<String> command = new ArrayList<String>();
    command.add(hostPython.getAbsolutePath());
    command.add(virtualenv.getAbsolutePath());
    command.add(root.getAbsolutePath());
    try {
      if (shell.execBlocking(command) != 0) {
        bail("Command returned non-zero exit code");
      }
    } catch (IOException e) {
      bail("Couldn't create virtualenv", e);
    }
  }

  private File getVirtualenvExecutable() {
    File preinstalled = path.which("virtualenv");
    if (preinstalled != null) {
      log.info("Found pre-installed virtualenv " + preinstalled);
      return preinstalled;
    }
    log.info("No virtualenv found, downloading.");

    try {
      File downloaded = File.createTempFile("virtualenv", ".py");
      shell.download(fallbackUrl, downloaded);
      return downloaded;
    } catch (IOException e) {
      bail("Couldn't download virtualenv", e);
    }
    return null;
  }

  private File getHostPython() {
    File hostPython = path.which("python");
    if (hostPython == null) {
      bail("No Python Found");
    }
    return hostPython;
  }

  private File getRoot(String givenRoot) {
    File root = null;
    if (givenRoot.equals("")) {
      root = getTemporaryDirectory();
    } else {
      root = new File(givenRoot);
      root.mkdirs();
    }
    log.info("Using virtualenv root: " + root);
    return root;
  }

  private File getTemporaryDirectory() {
    File temp = null;
    try {
      temp = File.createTempFile("virtualenv-", "");
      temp.delete();
      temp.mkdir();
    } catch (IOException e) {
      bail("Could not create temporary directory", e);
    }

    return temp;
  }

  private void bail(String message, Throwable t) {
    RuntimeException rte = null;
    if (t instanceof RuntimeException) {
      rte = (RuntimeException) t;
    } else {
      rte = new RuntimeException(t);
    }
    log.fatal(message, rte);
    throw rte;
  }

  private void bail(String message) {
    bail(message, new RuntimeException(message));
  }
}
