/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package au.com.customer.core.internal.services;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import au.com.customer.core.services.customer.ResourceResolverHelperService;
import au.com.customer.core.testcontext.CustomerAemContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.servlet.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class EventsRegisterServiceImplTest {

    private EventsRegisterServiceImpl eventsRegisterService;

    private WireMockServer wireMockServer;
    private int wireMockPort;
    private Resource pageRes;
    public final AemContext context = CustomerAemContext.newAemContext();
    private static final String CONTENT_ROOT = "/content";

    @Mock
    private RequestDispatcher requestDispatcher;

    @Mock
    private ResourceResolverHelperService helperService;

    @BeforeEach
    void setUp() {
        context.load().json("/form/events-service/test-content.json", CONTENT_ROOT);
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        wireMockPort = wireMockServer.port();
        setupStub();
        context.registerService(HttpClientBuilderFactory.class, HttpClientBuilder::create);
        context.registerService(ResourceResolverHelperService.class, helperService);
        context.request().setRequestDispatcherFactory(new MockRequestDispatcherFactory() {
            @Override
            public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
                return requestDispatcher;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
                return requestDispatcher;
            }
        });
        pageRes = context.currentResource("/content");
        context.currentResource(CONTENT_ROOT + "/container");
    }

    private void setupStub() {
        wireMockServer.stubFor(post(urlEqualTo("/form/endpoint"))
                .withRequestBody(equalTo("{}"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)));
        wireMockServer.stubFor(post(urlEqualTo("/form/endpoint"))
                .withRequestBody(equalTo("{\"text\":\"hello\"}"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)));
    }

    @Test
    void testDoPostWithSuccess() throws ServletException, IOException {
        Mockito.when(helperService.getUgcResolver()).thenReturn(context.resourceResolver());
        eventsRegisterService = context.registerInjectActivateService(new EventsRegisterServiceImpl());
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.of("name", "user name", "email", "email@test.com", "id", "id"));
        request.setAttribute("com.day.cq.wcm.foundation.forms.FormsHandlingServletHelper/resource", pageRes);
        eventsRegisterService.handleFormRequest(context.request(), context.response());
        assertEquals(302 , context.response().getStatus());
    }

    @Test
    void testDoPostWithFailure() throws ServletException, IOException {
        eventsRegisterService = context.registerInjectActivateService(new EventsRegisterServiceImpl());
        MockSlingHttpServletRequest request = context.request();
        request.setAttribute("com.day.cq.wcm.foundation.forms.FormsHandlingServletHelper/resource", pageRes);
        eventsRegisterService.handleFormRequest(context.request(), context.response());
        assertEquals(400 , context.response().getStatus());
    }

    @Test
    void testDoPostWithServerError() throws ServletException, IOException {
        eventsRegisterService = context.registerInjectActivateService(new EventsRegisterServiceImpl());
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.of("name", "user name", "email", "email@test.com", "id", "id"));
        request.setAttribute("com.day.cq.wcm.foundation.forms.FormsHandlingServletHelper/resource", pageRes);
        eventsRegisterService.handleFormRequest(context.request(), context.response());
        assertEquals(500 , context.response().getStatus());
    }

}
