package au.com.customer.core.services.events;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;
import javax.servlet.ServletException;

/**
 * This service is used for handling backend operations for the event registration process.
 */
public interface EventsRegisterService {

    /**
     * Fetches form data and submits to backend service for processing. This service also pushes the response to
     * JCR for each user. If the response is successful the service redirects user to the provided redirect URL.
     * In case of an error, the service displays error message.
     *
     * @param req request object
     * @param resp response object
     */
    void handleFormRequest(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
            throws ServletException, IOException;

}
