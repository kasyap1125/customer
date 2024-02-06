package au.com.customer.core.internal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import au.com.customer.core.services.customer.ResourceResolverHelperService;
import au.com.customer.core.testcontext.CustomerAemContext;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import javax.servlet.ServletException;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class EventsRegisterServiceImplTest {

    private EventsRegisterServiceImpl eventsRegisterService;
    private Resource pageRes;
    public final AemContext context = CustomerAemContext.newAemContext();
    private static final String CONTENT_ROOT = "/content";

    @Mock
    private ResourceResolverHelperService helperService;

    @BeforeEach
    void setUp() {
        context.load().json("/form/events-service/test-content.json", CONTENT_ROOT);
        context.registerService(ResourceResolverHelperService.class, helperService);
        pageRes = context.currentResource("/content");
        context.currentResource(CONTENT_ROOT + "/container");
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
