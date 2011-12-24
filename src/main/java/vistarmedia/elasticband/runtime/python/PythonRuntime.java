package vistarmedia.elasticband.runtime.python;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import vistarmedia.elasticband.os.Path;
import vistarmedia.elasticband.runtime.ElasticBandRuntime;

@Singleton
public class PythonRuntime extends ElasticBandRuntime {

  private File python;
  private File applicationRoot;

  @Inject
  public PythonRuntime(Path path,
      @Named("application.root") String applicationRoot) {
    super();
    File python = path.which("python");
    if (python == null) {
      throw new RuntimeException("Couldn't find Python executable");
    }
    this.python = python;
    this.applicationRoot = new File(applicationRoot);
  }

  protected ProcessBuilder getBuilder(String command) {
    List<String> cmd = new ArrayList<String>();
    cmd.add(python.getAbsolutePath());
    Collections.addAll(cmd, command.split("\\s+"));
    
    ProcessBuilder builder = new ProcessBuilder(cmd);
    builder.directory(getWorkingDirectory(applicationRoot));
    return builder;
  }
}
