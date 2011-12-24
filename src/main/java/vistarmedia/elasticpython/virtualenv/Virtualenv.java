//package vistarmedia.elasticpython.virtualenv;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//
//import javax.inject.Inject;
//import javax.inject.Named;
//import javax.inject.Singleton;
//
//import org.apache.log4j.Logger;
//
//import vistarmedia.elasticband.os.HostPython;
//import vistarmedia.elasticband.os.Python;
//import vistarmedia.elasticband.os.Shutil;
//
///**
// * <p>
// * A {@link Python} instance which is set up inside a virtual environment. If
// * the <cc>virtualenv</cc> executable can't be found, one will be downloaded,
// * and it will be executed with the {@link HostPython} install.
// * </p>
// * 
// * https://raw.github.com/pypa/virtualenv/master/virtualenv.py
// */
//@Singleton
//public class Virtualenv extends Python {
//
//  private Shutil              shutil;
//  private File                root;
//  private File                binDir;
//  private File                pip;
//  private File                python;
//
//  private static final Logger log = Logger.getLogger(Virtualenv.class);
//
//  @Inject
//  public Virtualenv(HostPython hostPython, Shutil shutil,
//      @Named("application.root") String applicationRoot,
//      @Named("python.virtualenv.url") String fallbackUrl,
//      @Named("application.requirements") String requirements) {
//    this.shutil = shutil;
//    File virtualenvFile;
//    try {
//      virtualenvFile = getVirtualenvFile(fallbackUrl);
//    } catch (Exception e) {
//      e.printStackTrace();
//      log.fatal("Problems making virtualenv", e);
//      throw new RuntimeException(e);
//    }
//    if (virtualenvFile == null) {
//      log.fatal("Couldn't find or download virtualenv");
//      throw new RuntimeException("Couldn't find or download virtualenv");
//    }
//
//    try {
//      this.root = setupRoot(hostPython, virtualenvFile);
//    } catch (IOException e) {
//      e.printStackTrace();
//      log.fatal("Problems making virtualenv", e);
//      throw new RuntimeException(e);
//    }
//    this.binDir = new File(root, "bin");
//    this.python = new File(binDir, "python");
//    this.pip = new File(binDir, "pip");
//
//    File reqFile = new File(applicationRoot, requirements);
//    installRequirements(reqFile);
//  }
//
//  private void installRequirements(File requirements) {
//    log.info("shutil: "+ this.shutil);
//    log.info("pip: "+ pip);
//    log.info("pip path: "+ pip.getAbsolutePath());
//    log.info("requirements: "+ requirements);
//    log.info("requirements path: "+ requirements.getAbsolutePath());
//    this.execBlocking(this.shutil, pip.getAbsolutePath(), "install", "-r",
//        requirements.getAbsolutePath());
//  }
//
//  /**
//   * <p>
//   * Either find <cc>virtualenv</cc> in the path, or download it to some
//   * temporary location. If anything goes wrong, this will return <cc>null</cc>.
//   * </p>
//   * 
//   * @param shutil
//   * @param fallbackUrl
//   * @return
//   */
//  private File getVirtualenvFile(String fallbackUrl) throws Exception {
//    File preinstalled = shutil.which("virtualenv");
//    if (preinstalled != null) {
//      return preinstalled;
//    }
//
//    File downloaded = File.createTempFile("virtualenv", ".py");
//    if (shutil.download(new URL(fallbackUrl), downloaded)) {
//      return downloaded;
//    }
//
//    return null;
//  }
//
//  private File setupRoot(Python hostPython, File virtualenvFile)
//      throws IOException {
//    File root = getTemporaryDirectory();
//    hostPython.execBlocking(shutil, virtualenvFile.getAbsolutePath(),
//        root.getAbsolutePath());
//    return root;
//  }
//
//  private File getTemporaryDirectory() throws IOException {
//    File temp = File.createTempFile("venv", "");
//    temp.delete();
//    temp.mkdir();
//    return temp;
//  }
//
//  @Override
//  protected File getPython() {
//    return this.python;
//  }
//}
