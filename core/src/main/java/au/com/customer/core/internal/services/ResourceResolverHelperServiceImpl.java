package au.com.customer.core.internal.services;

import au.com.customer.core.services.customer.ResourceResolverHelperService;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Resource Resolver Helper Service Impl.
 */
@Component(service = { ResourceResolverHelperService.class }, immediate = true)
@ServiceDescription("Resource Resolver Helper Service")
public class ResourceResolverHelperServiceImpl implements ResourceResolverHelperService {

    /**
     * Default Logger.
     */
    private static final Logger LOGGER
        = LoggerFactory.getLogger(ResourceResolverHelperServiceImpl.class);

    /**
     * ResourceResolverFactory reference.
     */
    @Reference
    private ResourceResolverFactory resolverFactory;


    public ResourceResolver getUgcResolver() {
        try {
            final Map<String, Object> param = new HashMap<>();
            param.put(ResourceResolverFactory.SUBSERVICE, "customer-ugc-write-service");
            return resolverFactory.getServiceResourceResolver(param);
        } catch (final LoginException e) {
            LOGGER.error("Error while getting the ResourceResolver ", e);
        }
        return null;
    }

}