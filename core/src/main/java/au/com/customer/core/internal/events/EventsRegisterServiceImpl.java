package au.com.customer.core.internal.events;

import static au.com.customer.core.internal.CustomerUtils.getFormFieldNames;
import static au.com.customer.core.internal.CustomerUtils.getMappedRedirect;

import au.com.customer.core.internal.CustomerUtils;
import au.com.customer.core.services.customer.ResourceResolverHelperService;
import au.com.customer.core.services.events.EventsRegisterService;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.foundation.forms.FormsHandlingRequest;
import com.day.cq.wcm.foundation.forms.FormsHandlingServletHelper;
import com.day.cq.wcm.foundation.forms.ValidationInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

@Component(service = { EventsRegisterService.class })
@ServiceDescription("Event registration service")
public class EventsRegisterServiceImpl implements EventsRegisterService {

    private static final Logger LOG = LoggerFactory.getLogger(EventsRegisterServiceImpl.class);

    private static final String ATTR_RESOURCE = FormsHandlingServletHelper.class.getName() + "/resource";

    private static final String PN_FORM_ENDPOINT_URL = "backendUrl";

    private static final Set<String> INTERNAL_PARAMETER = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ":formstart",
            "_charset_",
            ":redirect",
            ":cq_csrf_token"
    )));

    @Reference
    ResourceResolverHelperService resolverHelperService;

    public void handleFormRequest(final SlingHttpServletRequest req,
                                  final SlingHttpServletResponse resp) throws ServletException, IOException {
        boolean processFormApiSuccess = false;
        final Resource formContainerResource = req.getResource();
        final ValueMap valueMap = formContainerResource.adaptTo(ValueMap.class);
        JSONObject formData = new JSONObject();
        if (valueMap != null) {
            String endPointUrl = valueMap.get(PN_FORM_ENDPOINT_URL, String.class);
            String response = null;
            String result = org.apache.commons.lang3.StringUtils.EMPTY;
            if (StringUtils.isNotEmpty(endPointUrl)) {
                try {
                    formData = getJsonOfRequestParameters(req);
                    if (formData.length() > 0 && formData.has("name")) {
                        response = CustomerUtils.makeHttpRequest(endPointUrl, formData);
                        if (StringUtils.isNotBlank(response)) {
                            final Map<String, String> resultMap = new ObjectMapper().readValue(response, HashMap.class);
                            result = resultMap.get("result");
                            if (StringUtils.equals(result, "success")) {
                                processFormApiSuccess = true;
                            } else {
                                saveDataToJcr(formData, result, formContainerResource);
                            }
                        }
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            saveDataAndsendRedirect(formData, result, valueMap, req, resp, processFormApiSuccess);
        }
    }

    private JSONObject getJsonOfRequestParameters(final SlingHttpServletRequest request) throws JSONException {
        Set<String> formFieldNames = getFormFieldNames(request);
        JSONObject jsonObj = new JSONObject();
        Map<String, String[]> params = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (!INTERNAL_PARAMETER.contains(entry.getKey()) && formFieldNames.contains(entry.getKey())) {
                String[] v = entry.getValue();
                Object o = (v.length == 1) ? v[0] : v;
                jsonObj.put(entry.getKey(), o);
            }
        }
        return jsonObj;
    }

    private void saveDataAndsendRedirect(final JSONObject formData, final String apiResp, final ValueMap valueMap,
                                         final SlingHttpServletRequest request,
                                         final SlingHttpServletResponse response, final boolean processFormApiSuccess)
            throws ServletException {
        String redirect = getMappedRedirect(valueMap.get("redirect", String.class), request.getResourceResolver());
        String errorMessage = valueMap.get("eventsErrorMessage", String.class);
        FormsHandlingRequest formRequest = new FormsHandlingRequest(request);
        final Resource formResource = (Resource) request.getAttribute(ATTR_RESOURCE);
        try {
            if (StringUtils.isNotEmpty(redirect) && processFormApiSuccess) {
                saveDataToJcr(formData, apiResp, formResource);
                response.sendRedirect(redirect);
            } else {
                if (!processFormApiSuccess && StringUtils.isNotEmpty(errorMessage)) {
                    ValidationInfo validationInfo = ValidationInfo.createValidationInfo(request);
                    validationInfo.addErrorMessage(null, errorMessage);
                }
                request.removeAttribute(ATTR_RESOURCE);
                request.removeAttribute(ComponentContext.BYPASS_COMPONENT_HANDLING_ON_INCLUDE_ATTRIBUTE);
                RequestDispatcher requestDispatcher = request.getRequestDispatcher(formResource);
                if (requestDispatcher != null) {
                    requestDispatcher.forward(formRequest, response);
                } else {
                    throw new IOException("can't get request dispatcher to forward the response");
                }
            }
        } catch (IOException var13) {
            LOG.error("Error redirecting to {}", redirect);
        }
    }

    private void saveDataToJcr(final JSONObject formData, final String apiResp, final Resource formResource) {
        try (ResourceResolver resourceResolver = resolverHelperService.getUgcResolver()) {
            if (null != resourceResolver) {
                final Resource resource = ResourceUtil.getOrCreateResource(resourceResolver, "/content/usergenerated/"
                                + formResource.getPath().replace("/content", StringUtils.EMPTY),
                        "sling:Folder", "sling:Folder", false);
                final String userName = (String) formData.get("name");
                final Resource userRes = ResourceUtil.getOrCreateResource(resourceResolver,
                        resource.getPath() + "/" + userName.replaceAll(" ", "-") + System.currentTimeMillis(),
                        "sling:Folder", "sling:Folder", true);
                final ModifiableValueMap modifiableValueMap = userRes.adaptTo(ModifiableValueMap.class);
                modifiableValueMap.putAll(new ObjectMapper().readValue(formData.toString(), HashMap.class));
                modifiableValueMap.put("apiRespinse", apiResp);
                resourceResolver.commit();
            } else {
                LOG.error("Unable to save the user information as the service user ");
            }
        }  catch (PersistenceException | JSONException | JsonProcessingException e) {
            LOG.error("Unable to save data");
        }
    }

}
