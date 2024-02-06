package au.com.customer.core.internal.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import au.com.customer.core.services.customer.ResourceResolverHelperService;
import au.com.customer.core.testcontext.CustomerAemContext;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class MockApiTest {

    private MockApi mockApi;
    public final AemContext context = CustomerAemContext.newAemContext();

    @Mock
    private ResourceResolverHelperService helperService;

    @BeforeEach
    void setUp() {
       mockApi = context.registerInjectActivateService(new MockApi());
    }

    @Test
    void testDoPostSuccess() throws IOException {
        mockApi.doPost(context.request(), context.response());
        assertEquals(200 , context.response().getStatus());
    }

    @Test
    void testDoPostFailure() throws IOException {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.of("failure", "true"));
        mockApi.doPost(request, context.response());
        assertEquals(500 , context.response().getStatus());
    }


}
