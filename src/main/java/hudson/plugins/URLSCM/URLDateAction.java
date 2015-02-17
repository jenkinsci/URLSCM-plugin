package hudson.plugins.URLSCM;

import hudson.model.AbstractBuild;
import hudson.scm.SCMRevisionState;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class URLDateAction extends SCMRevisionState {
    private static final long serialVersionUID = 1L;

    private final HashMap<String, Long> lastModified = new HashMap<String, Long>();

    private final AbstractBuild<?, ?> build;

    protected URLDateAction(final AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    public long getLastModified(final String u) {
        final Long l = lastModified.get(u);
        if (l == null) {
            return 0;
        }
        return l;
    }

    public void setLastModified(final String u, final long l) {
        lastModified.put(u, l);
    }

    public Map<String, String> getUrlDates() {
        final Map<String, String> ret = new HashMap<String, String>();
        for (final Map.Entry<String, Long> e : lastModified.entrySet()) {
            final long sinceEpoch = e.getValue();
            if (sinceEpoch == 0) {
                ret.put(e.getKey(), "Last-modified not supported");
            }
            else {
                ret.put(e.getKey(),
                        DateFormat.getInstance().format(new Date(sinceEpoch)));
            }
        }
        return ret;
    }

    @Override
    public String getDisplayName() {
        return "URL Modification Dates";
    }

    @Override
    public String getIconFileName() {
        return "save.gif";
    }

    public String getSearchUrl() {
        return getUrlName();
    }

    @Override
    public String getUrlName() {
        return "urlDates";
    }

    public void doIndex(final StaplerRequest req, final StaplerResponse rsp)
            throws IOException, ServletException {
        req.getView(this, chooseAction()).forward(req, rsp);
    }

    protected String chooseAction() {
        return "tagForm.jelly";
    }
}
