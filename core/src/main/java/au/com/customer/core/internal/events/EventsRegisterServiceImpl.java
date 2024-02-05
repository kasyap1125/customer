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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
        JsonObject formData = new JsonObject();
        if (valueMap != null) {
            String endPointUrl = valueMap.get(PN_FORM_ENDPOINT_URL, String.class);
            String result = StringUtils.EMPTY;
            if (StringUtils.isNotEmpty(endPointUrl)) {
                formData = getJsonOfRequestParameters(req);
                if (formData.size() > 0 && formData.has("name")) {
                    final String response = CustomerUtils.makeHttpRequest(endPointUrl, formData);
                    if (StringUtils.isNotBlank(response)) {
                        final JsonObject apiResp = JsonParser.parseString(response).getAsJsonObject();
                        result = apiResp.get("result").getAsString();
                        if (StringUtils.equals(result, "success")) {
                            processFormApiSuccess = true;
                        } else {
                            saveDataToJcr(formData, result, formContainerResource, req, false);
                        }
                    }
                }
            }
            saveDataAndsendRedirect(formData, result, valueMap, req, resp, processFormApiSuccess);
        }
    }

    private JsonObject getJsonOfRequestParameters(final SlingHttpServletRequest request) {
        Set<String> formFieldNames = getFormFieldNames(request);
        JsonObject jsonObj = new JsonObject();
        Map<String, String[]> params = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (!INTERNAL_PARAMETER.contains(entry.getKey()) && formFieldNames.contains(entry.getKey())) {
                String[] v = entry.getValue();
                String o = (v.length == 1) ? v[0] : Arrays.toString(v);
                jsonObj.addProperty(entry.getKey(), o);
            }
        }
        return jsonObj;
    }

    private void saveDataAndsendRedirect(final JsonObject formData, final String apiResp, final ValueMap valueMap,
                                         final SlingHttpServletRequest request,
                                         final SlingHttpServletResponse response, final boolean processFormApiSuccess)
            throws ServletException {
        String redirect = getMappedRedirect(valueMap.get("redirect", String.class), request.getResourceResolver());
        String errorMessage = valueMap.get("eventsErrorMessage", String.class);
        FormsHandlingRequest formRequest = new FormsHandlingRequest(request);
        final Resource formResource = (Resource) request.getAttribute(ATTR_RESOURCE);
        try {
            if (StringUtils.isNotEmpty(redirect) && processFormApiSuccess) {
                if (saveDataToJcr(formData, apiResp, formResource, request, true)) {
                    response.sendRedirect(redirect);
                }
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

    private boolean saveDataToJcr(final JsonObject formData, final String apiResp, final Resource formResource,
                                  final SlingHttpServletRequest request, final boolean processFormApiSuccess) {
        boolean currentFlag = processFormApiSuccess;
        try (ResourceResolver resourceResolver = resolverHelperService.getUgcResolver()) {
            if (null != resourceResolver) {
                final Resource resource = ResourceUtil.getOrCreateResource(resourceResolver, "/content/usergenerated/"
                                + formResource.getPath().replace("/content", StringUtils.EMPTY),
                        "sling:Folder", "sling:Folder", false);
                final String id = formData.get("id").getAsString();
                final Resource userRes = ResourceUtil.getOrCreateResource(resourceResolver,
                        resource.getPath() + "/" + id + System.currentTimeMillis(),
                        "sling:Folder", "sling:Folder", true);
                final ModifiableValueMap modifiableValueMap = userRes.adaptTo(ModifiableValueMap.class);
                if (null != modifiableValueMap) {
                    modifiableValueMap.put("userId", id);
                    modifiableValueMap.put("apiResponse", apiResp);
                    resourceResolver.commit();
                }
            } else {
                currentFlag = false;
                ValidationInfo validationInfo = ValidationInfo.createValidationInfo(request);
                validationInfo.addErrorMessage(null, "Something wrong in saving data");
                LOG.error("Unable to save the user information as the write service is missing permissions, "
                        + "please update access rights to service user.");
            }
        }  catch (PersistenceException e) {
            LOG.error("Unable to save data");
        }
        return currentFlag;
    }

}
