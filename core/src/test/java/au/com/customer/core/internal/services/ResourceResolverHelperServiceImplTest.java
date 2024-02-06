package au.com.customer.core.internal.services;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class ResourceResolverHelperServiceImplTest {

    @Mock
    private ResourceResolverFactory factory;

    @InjectMocks
    ResourceResolverHelperServiceImpl service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetResolver() throws LoginException {
        ResourceResolver mockedResourceResolver = mock(ResourceResolver.class);
        when(factory.getServiceResourceResolver(anyMap())).thenReturn(mockedResourceResolver);
        ResourceResolver result = service.getUgcResolver();
        verify(factory).getServiceResourceResolver(anyMap());
        assert(result != null);
    }

    @Test
    public void testGetUgcResolver_LoginException() throws LoginException {
        when(factory.getServiceResourceResolver(anyMap())).thenThrow(LoginException.class);
        ResourceResolver result = service.getUgcResolver();
        verify(factory).getServiceResourceResolver(anyMap());
        assert(result == null);
    }

}
