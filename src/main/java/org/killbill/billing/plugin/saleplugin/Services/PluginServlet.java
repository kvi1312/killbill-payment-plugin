package org.killbill.billing.plugin.saleplugin.Services;

import com.google.inject.Singleton;
import java.util.Optional;
import javax.inject.Named;
import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.Path;
import org.killbill.billing.tenant.api.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
@Path("/")
public class PluginServlet {
    private static final Logger _logger= LoggerFactory.getLogger(PluginServlet.class);

    public PluginServlet(){}
    
    @GET
    public void Index(@Local @Named("killbill_tenant") final Optional<Tenant> tenant){
        _logger.info("Hello from sale-plugin");
        if(tenant != null && tenant.isPresent()){
            _logger.info("Tenant is available with id {}", tenant.get().getId());
        }else {
            _logger.warn("No tenant found!!!");
        }
    }
}
