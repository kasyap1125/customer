package au.com.customer.core.internal;

import com.day.cq.wcm.foundation.forms.FormsHandlingServletHelper;

import java.util.Set;

/**
 * Final class to define the constants.
 */
public final class CustomerConstants {
    
    private CustomerConstants() {}

    public static final String ATTR_RESOURCE = FormsHandlingServletHelper.class.getName() + "/resource";
    public static final String PN_FORM_ENDPOINT_URL = "backendUrl";
    public static final String REDIRECT_PARAM_NAME = "redirect";
    public static final String EVENTS_ERROR_MESSAGE_PARAM_NAME = "eventsErrorMessage";
    public static final String FORM_FIELD_NAME = "name";
    public static final String FORM_FIELD_EMAIL = "email";
    public static final String CONTENT_PATH_PREFIX = "/content/usergenerated/";
    public static final String CONTENT_ROOT = "/content";
    public static final String USER_ID_FIELD = "userId";
    public static final String API_RESPONSE_FIELD = "apiResponse";
    public static final String FORM_FIELD_ID = "id";
    public static final String FORWARD_SLASH = "/";
    public static final Set<String> INTERNAL_PARAMETERS = Set.of(":formstart", "_charset_", ":redirect", ":cq_csrf_token");
}
