package vistarmedia.elasticband.os;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.Logger;

@Singleton
public class Path {

  private static final Logger log = Logger.getLogger(Path.class);

  private String[]            path;

  @Inject
  public Path() {
    String pathStr = System.getenv("PATH");
    this.path = pathStr.split(File.pathSeparator);
  }

  /**
   * Finds an executable in the path, or returns an ever-helpful <cc>null</cc>.
   * 
   * @param name
   *          Name of the executable to find in the path
   * @return
   */
  public File which(String name) {
    File executable = null;
    for (String pathDir : path) {
      File file = new File(pathDir, name);
      if (file.isFile()) {
        executable = file;
        break;
      }
    }
    if(executable != null) {
      log.info("Found '"+ name +"' at "+ executable);
    } else {
      log.warn("Couldn't find "+ name);
    }
    return executable;
  }
}
