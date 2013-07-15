package hudson.plugins.URLSCM;

import static hudson.Util.fixEmpty;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.NullChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class URLSCM extends hudson.scm.SCM {
    private final ArrayList<URLTuple> urls = new ArrayList<URLTuple>();

    private final boolean             clearWorkspace;

    public URLSCM(final String[] u, final boolean clear) {
        for (final String element : u) {
            urls.add(new URLTuple(element));
        }
        clearWorkspace = clear;
    }

    public URLTuple[] getUrls() {
        return urls.toArray(new URLTuple[urls.size()]);
    }

    public boolean isClearWorkspace() {
        return clearWorkspace;
    }

    @Override
    public boolean checkout(final AbstractBuild<?, ?> build,
            final Launcher launcher, final FilePath workspace,
            final BuildListener listener, final File changelogFile)
            throws IOException, InterruptedException {
        if (clearWorkspace) {
            workspace.deleteContents();
        }

        Map<String, String> buildParameters = build.getBuildVariables();

        final URLDateAction dates = new URLDateAction(build);

        for (final URLTuple tuple : urls) {
            final String urlString = expandUrl(tuple, buildParameters);
            InputStream is = null;
            OutputStream os = null;
            try {
                final URL url = new URL(urlString);
                final URLConnection conn = url.openConnection();
                conn.setUseCaches(false);
                dates.setLastModified(urlString, conn.getLastModified());
                is = conn.getInputStream();
                final String path = new File(url.getPath()).getName();
                listener.getLogger().append(
                        "Copying " + urlString + " to " + path + "\n");
                os = workspace.child(path).write();
                final byte[] buf = new byte[8192];
                int i = 0;
                while ((i = is.read(buf)) != -1) {
                    os.write(buf, 0, i);
                }
            }
            catch (final Exception e) {
                listener.error("Unable to copy " + urlString + "\n"
                        + e.getMessage());
                return false;
            }
            finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            }
            createEmptyChangeLog(changelogFile, listener, "log");
        }
        build.addAction(dates);

        return true;
    }

    private static String expandUrl(final URLTuple urlTuple, final Map<String, String> buildParameters) {
        final String rawUrl = urlTuple.getUrl();
        return URLParameter.substituteAll(rawUrl, buildParameters);
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new NullChangeLogParser();
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        // this plugin does the polling work via the data in the Run
        // the data in the workspace is not used
        return false;
    }

    @Override
    public boolean pollChanges(final AbstractProject project,
            final Launcher launcher, final FilePath workspace,
            final TaskListener listener) throws IOException,
            InterruptedException {
        boolean change = false;
        final Run lastBuild = project.getLastBuild();
        if (lastBuild == null) {
            return true;
        }
        final URLDateAction dates = lastBuild.getAction(URLDateAction.class);
        if (dates == null) {
            return true;
        }

        for (final URLTuple tuple : urls) {
            final String urlString = tuple.getUrl();
            try {
                final URL url = new URL(urlString);
                final URLConnection conn = url.openConnection();
                conn.setUseCaches(false);

                final long lastMod = conn.getLastModified();
                final long lastBuildMod = dates.getLastModified(urlString);
                if (lastBuildMod != lastMod) {
                    listener.getLogger().println(
                            "Found change: " + urlString + " modified "
                                    + new Date(lastMod)
                                    + " previous modification was "
                                    + new Date(lastBuildMod));
                    change = true;
                }
            }
            catch (final Exception e) {
                listener.error("Unable to check " + urlString + "\n"
                        + e.getMessage());
            }
        }
        return change;
    }

    public static final class URLTuple {
        private final String urlString;

        public URLTuple(final String s) {
            urlString = s;
        }

        public String getUrl() {
            return urlString;
        }
    }

    @Extension
    public static final class DescriptorImpl extends SCMDescriptor<URLSCM> {

        public DescriptorImpl() {
            super(URLSCM.class, null);
            load();
        }

        @Override
        public String getDisplayName() {
            return "URL Copy";
        }

        @Override
        public SCM newInstance(final StaplerRequest req,
                final JSONObject formData) throws FormException {
            return new URLSCM(req.getParameterValues("URL.url"),
                    req.getParameter("URL.clear") != null);
        }

        @Override
        public boolean configure(final StaplerRequest req,
                final JSONObject formData) throws FormException {
            return true;
        }

        public FormValidation doUrlCheck(@QueryParameter final String value)
                throws IOException, ServletException {
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.ok();
            }

            return new FormValidation.URLCheck() {
                @Override
                protected FormValidation check() throws IOException,
                        ServletException {
                    final String url = fixEmpty(value);

                    //parameters cannot be validated here, so allow
                    Set<URLParameter> parameters = URLParameter.getParameters(url);
                    if(! parameters.isEmpty()) {
                        return FormValidation.ok("URL contains parameters");
                    }

                    URL u = null;
                    try {
                        u = new URL(url);
                        open(u);
                    }
                    catch (final Exception e) {
                        return FormValidation.error("Cannot open " + url);
                    }
                    final String path = new File(u.getPath()).getName();
                    if (path.length() == 0) {
                        return FormValidation
                                .error("URL does not contain filename: " + url);
                    }
                    return FormValidation.ok();
                }
            }.check();
        }
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(
            final AbstractBuild<?, ?> arg0, final Launcher arg1,
            final TaskListener arg2) throws IOException, InterruptedException {
        return null;
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(
            final AbstractProject<?, ?> arg0, final Launcher arg1,
            final FilePath arg2, final TaskListener arg3,
            final SCMRevisionState arg4) throws IOException,
            InterruptedException {
        return PollingResult.BUILD_NOW;
    }
}
