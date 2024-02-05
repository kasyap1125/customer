package au.com.customer.core.internal;

import com.adobe.cq.wcm.core.components.models.form.Field;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

public class CustomerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(CustomerUtils.class);

    public static final String HTML_SUFFIX = ".html";

    /**
     * Utility method to make an HTTP call. This method assumes that the mocked API is from local AEM instance
     * and applies basic authentication with default credentials
     *
     * @param apiUrl API URL
     * @param requestBody request body from front end
     * @return response from the API
     * @throws IOException if the IO operations fail
     */
    public static String makeHttpRequest(String apiUrl, JsonObject requestBody) throws IOException {
        HttpURLConnection connection = getHttpURLConnection(apiUrl);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        try (InputStream inputStream = connection.getInputStream()) {
            String responseData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            LOG.debug("Http Call returned in response: {}", responseData);
            return responseData;
        }
    }

    private static HttpURLConnection getHttpURLConnection(final String apiUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Basic " + encodeCredentials());
        return connection;
    }

    /**
     * For the current project specific needs, this method assumes the default admin credentials to call Mock API
     *
     * @return encoded credentials
     */
    private static String encodeCredentials() {
        String credentials = "admin" + ":" + "admin";
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public static Set<String> getFormFieldNames(SlingHttpServletRequest request) {
        Set<String> formFieldNames = new LinkedHashSet<>();
        collectFieldNames(request.getResource(), formFieldNames);
        return formFieldNames;
    }

    private static void collectFieldNames(Resource resource, Set<String> fieldNames) {
        if (resource != null) {
            for (Resource child : resource.getChildren()) {
                String name = child.getValueMap().get(Field.PN_NAME, String.class);
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(name)) {
                    fieldNames.add(name);
                }
                collectFieldNames(child, fieldNames);
            }
        }
    }
    public static String getMappedRedirect(String redirect, ResourceResolver resourceResolver) {
        String mappedRedirect = null;
        if (StringUtils.isNotEmpty(redirect)) {
            if (StringUtils.endsWith(redirect, HTML_SUFFIX)) {
                Resource resource = resourceResolver.resolve(redirect);
                if (!(resource instanceof NonExistingResource)) {
                    mappedRedirect = redirect;
                }
            } else {
                Resource resource = resourceResolver.getResource(redirect);
                if (resource != null) {
                    redirect += HTML_SUFFIX;
                    mappedRedirect = resourceResolver.map(redirect);
                }
            }
        }
        return mappedRedirect;
    }

}