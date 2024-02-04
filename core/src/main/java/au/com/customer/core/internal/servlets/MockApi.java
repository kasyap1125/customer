package au.com.customer.core.internal.servlets;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.io.IOException;
import javax.servlet.Servlet;

/**
 * Mock API which acts as a backend service. Pass failure = true as query parameter to simulate error scenario.
 */
@Component(service = { Servlet.class },
        property = {
        "sling.servlet.paths=" + "/bin/services/mockApi",
        "sling.servlet.methods=POST",
        "sling.servlet.extensions=json"}, immediate = true)
@ServiceDescription("Mocked API")
public class MockApi extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(final SlingHttpServletRequest req,
                          final SlingHttpServletResponse resp) throws IOException {
        final String failure = StringUtils.isNotEmpty(req.getParameter("failure"))
                ? req.getParameter("failure") : StringUtils.EMPTY;
        resp.setContentType("application/json");
        if (StringUtils.equals("true", failure)) {
            resp.getWriter().write("{\"result\":\"failure\"}");
        } else {
            resp.getWriter().write("{\"result\":\"success\"}");
        }
    }
}
