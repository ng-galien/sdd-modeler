package io.statemodeler.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "verify",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class VerifyMojo extends AbstractSddMojo {

    @Override
    public void execute() throws MojoExecutionException {
        // Regenerate sources and DDL to ensure artifacts are current before project tests run.
        new GenerateMojo().execute();
        new GenerateDdlMojo().execute();
        getLog().info("SDD verify completed: generation refreshed; project tests should now run.");
    }
}
