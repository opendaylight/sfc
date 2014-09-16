package org.opendaylight.sfc.ui;

import java.io.IOException;
import java.net.URL;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SfcUiHttpContext implements HttpContext {

    private static final Logger LOG = LoggerFactory.getLogger(SfcUiHttpContext.class);

    private final HttpContext defaultHttpContext;
    private final String root;

    protected SfcUiHttpContext(HttpContext defaultHttpContext, String root) {
        this.defaultHttpContext = defaultHttpContext;
        this.root = root; // should end with '/'
    }

    @Override
    public boolean handleSecurity(javax.servlet.http.HttpServletRequest hsr, javax.servlet.http.HttpServletResponse hsr1) throws IOException {
        return defaultHttpContext.handleSecurity(hsr, hsr1);
    }

    @Override
    public URL getResource(String string) {
        String reqested = string;

        if (root.equals(string)) { // 'serve' root/index.html inplace of root/
            reqested = root + "index.html";
            LOG.debug("getResource: " + string);
        }

        return defaultHttpContext.getResource(reqested);
    }

    @Override
    public String getMimeType(String string) {
        return defaultHttpContext.getMimeType(string);
    }

}
