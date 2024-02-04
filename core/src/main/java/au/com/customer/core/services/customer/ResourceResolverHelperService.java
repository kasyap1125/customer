package au.com.customer.core.services.customer;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * A helper service to get the resolver instance for a given service.
 *
 */
public interface ResourceResolverHelperService {

    /**
     * Return the user generated service resource resolver.
     *
     * @return resource resolver.
     */
    ResourceResolver getUgcResolver();

}
