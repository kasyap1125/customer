package au.com.customer.core.internal.services;

import au.com.customer.core.internal.CustomerConstants;
import au.com.customer.core.internal.CustomerUtils;
import au.com.customer.core.internal.exception.ErrorCode;
import au.com.customer.core.internal.exception.SubmissionFailureException;
import au.com.customer.core.services.customer.ResourceResolverHelperService;
import au.com.customer.core.services.events.EventsRegisterService;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.foundation.forms.FormsHandlingRequest;
import com.day.cq.wcm.foundation.forms.ValidationInfo;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@Component(service = { EventsRegisterService.class })
@ServiceDescription("Event registration service")
public class EventsRegisterServiceImpl implements EventsRegisterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventsRegisterServiceImpl.class);

    @Reference
    ResourceResolverHelperService resolverHelperService;

    public void handleFormRequest(final SlingHttpServletRequest req,
                                  final SlingHttpServletResponse resp) throws ServletException, IOException {
        try {
            boolean processFormApiSuccess = false;
            final Resource formContainerResource = req.getResource();
            final ValueMap valueMap = formContainerResource.adaptTo(ValueMap.class);
            if (valueMap != null) {
                String endPointUrl = valueMap.get(CustomerConstants.BACKEND_URL, String.class);
                if (StringUtils.isNotEmpty(endPointUrl)) {
                    final JsonObject formData = createJsonOfRequestParameters(req);
                    if (isValidRequest(formData)) {
                        final JsonObject response = CustomerUtils.makeHttpRequest(endPointUrl, formData);
                        String result = null;
                        final int responseCode = response.get("status").getAsInt();
                        if (responseCode >= 200 && responseCode < 300) {
                            result = response.get("response").getAsString();
                            processFormApiSuccess = true;
                        }
                        handleDataAndRedirect(formData, result, valueMap, req, resp, processFormApiSuccess);
                        return;
                    }
                }
            }
            LOGGER.error("Unable to get form container properties");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (final SubmissionFailureException se) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            LOGGER.error("Error in submitting the form {}", se.getMessage(), se.getCause());
        }
    }

    private static boolean isValidRequest(final JsonObject formData) {
        return formData.size() > 0 && formData.has(CustomerConstants.FORM_FIELD_NAME)
                && formData.has(CustomerConstants.FORM_FIELD_EMAIL) && formData.has(CustomerConstants.FORM_FIELD_ID);
    }

    private JsonObject createJsonOfRequestParameters(final SlingHttpServletRequest request) {
        Set<String> formFieldNames = CustomerUtils.getFormFieldNames(request);
        JsonObject jsonObj = new JsonObject();
        Map<String, String[]> params = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (!CustomerConstants.INTERNAL_PARAMETERS.contains(entry.getKey()) && formFieldNames.contains(entry.getKey())) {
                String[] v = entry.getValue();
                String o = (v.length == 1) ? v[0] : StringUtils.EMPTY;
                jsonObj.addProperty(entry.getKey(), o);
            }
        }
        return jsonObj;
    }

    private void handleDataAndRedirect(final JsonObject formData, final String apiResp, final ValueMap valueMap,
                                       final SlingHttpServletRequest request,
                                       final SlingHttpServletResponse response, final boolean processFormApiSuccess)
            throws ServletException, SubmissionFailureException {
        final Resource formResource = (Resource) request.getAttribute(CustomerConstants.ATTR_RESOURCE);
        try {
            if (processFormApiSuccess && saveToJcr(formData, apiResp, formResource)) {
                final String redirect = CustomerUtils.getMappedRedirect(valueMap.get(
                        CustomerConstants.REDIRECT_PARAM_NAME, String.class), request.getResourceResolver());
                if (StringUtils.isNotEmpty(redirect)) {
                    response.sendRedirect(redirect);
                } else {
                    throw new SubmissionFailureException(ErrorCode.REDIRECT_NOT_CONFIGURED);
                }
            } else {
                final String errorMessage = valueMap.get(CustomerConstants.EVENTS_ERROR_MESSAGE_PARAM_NAME, String.class);
                if (StringUtils.isNotEmpty(errorMessage)) {
                    ValidationInfo validationInfo = ValidationInfo.createValidationInfo(request);
                    validationInfo.addErrorMessage(null, errorMessage);
                }
                forwardRequest(request, response, formResource);
            }
        } catch (final IOException e) {
            throw new SubmissionFailureException(e.getCause(), ErrorCode.FAILED_REDIRECT);
        }
    }

    private static void forwardRequest(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                                       final Resource formResource) throws ServletException,
            IOException, SubmissionFailureException {
        request.removeAttribute(CustomerConstants.ATTR_RESOURCE);
        request.removeAttribute(ComponentContext.BYPASS_COMPONENT_HANDLING_ON_INCLUDE_ATTRIBUTE);
        RequestDispatcher requestDispatcher = request.getRequestDispatcher(formResource);
        if (requestDispatcher != null) {
            FormsHandlingRequest formRequest = new FormsHandlingRequest(request);
            requestDispatcher.forward(formRequest, response);
        } else {
            throw new SubmissionFailureException(ErrorCode.DISPATCH_FAILED);
        }
    }

    private boolean saveToJcr(final JsonObject formData, final String apiResp, final Resource formResource)
            throws SubmissionFailureException {
        boolean savedInJcr = false;
        try (ResourceResolver resourceResolver = resolverHelperService.getUgcResolver()) {
            if (resourceResolver != null) {
                final String path = CustomerConstants.CONTENT_PATH_PREFIX + formResource.getPath()
                        .replace(CustomerConstants.CONTENT_ROOT, StringUtils.EMPTY);
                final Resource resource = ResourceUtil.getOrCreateResource(resourceResolver, path, JcrResourceConstants.NT_SLING_FOLDER,
                        JcrResourceConstants.NT_SLING_FOLDER, false);
                final String id = formData.get(CustomerConstants.FORM_FIELD_ID).getAsString();
                final Resource userRes = ResourceUtil.getOrCreateResource(resourceResolver, resource.getPath()
                                + CustomerConstants.FORWARD_SLASH + id + System.currentTimeMillis(),
                        JcrResourceConstants.NT_SLING_FOLDER ,JcrResourceConstants.NT_SLING_FOLDER, false);
                final ModifiableValueMap modifiableValueMap = userRes.adaptTo(ModifiableValueMap.class);
                if (modifiableValueMap != null) {
                    modifiableValueMap.put(CustomerConstants.USER_ID_FIELD, id);
                    modifiableValueMap.put(CustomerConstants.API_RESPONSE_FIELD, apiResp);
                    resourceResolver.commit();
                    savedInJcr = true;
                }
            } else {
                throw new SubmissionFailureException(ErrorCode.RESOLVER_NOT_FOUND);
            }
        } catch (final PersistenceException e) {
            throw new SubmissionFailureException(e, ErrorCode.RESOLVER_NOT_FOUND);
        }
        return savedInJcr;
    }
}