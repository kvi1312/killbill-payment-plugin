package org.killbill.billing.plugin.saleplugin.Services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Named;
import org.jooby.mvc.*;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.plugin.api.PluginCallContext;
import org.killbill.billing.plugin.saleplugin.Api.PaymentApi;
import org.killbill.billing.plugin.saleplugin.Dto.PaymentRequest;
import org.killbill.billing.plugin.saleplugin.Extensions.CommonVariables;
import org.killbill.billing.plugin.saleplugin.Extensions.SalePluginPaymentTransactionInfoPlugin;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.callcontext.CallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
@Path("/")
public class PluginServlet {
    private static final Logger _logger= LoggerFactory.getLogger(PluginServlet.class);
    private final OSGIKillbillClock clock;
    private final PaymentApi paymentApi;

    @Inject
    public PluginServlet(OSGIKillbillClock clock, PaymentApi paymentApi){
        this.clock = clock;
        this.paymentApi = paymentApi;
    }
    
    @GET
    public void hello(@Local @Named("killbill_tenant") final Optional<Tenant> tenant){
        _logger.info("Hello from sale-plugin");
        if(tenant != null && tenant.isPresent()){
            _logger.info("Tenant is available with id {}", tenant.get().getId());
        }else {
            _logger.warn("No tenant found!!!");
        }
    }

    @POST
    @Path("/checkout")
    public SalePluginPaymentTransactionInfoPlugin testPayment(@Named("kbAccountId") final UUID kbAccountId, @Local @Named("killbill_tenant") final Tenant tenant, @Body PaymentRequest request) {
        _logger.info("Received payment test request: kbAccountId {}", kbAccountId);
        try {
            final CallContext context = new PluginCallContext(CommonVariables.PLUGIN_NAME, clock.getClock().getUTCNow(), kbAccountId, tenant.getId());
            UUID paymentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID paymentMethodId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal(request.amount);
            Currency currency = Currency.valueOf(request.currency);

            switch (request.action.toLowerCase()) {
                case "authorize":
                    return paymentApi.authorizePayment(kbAccountId, paymentId, transactionId, paymentMethodId, amount, currency, null, context);
                case "purchase":
                    return paymentApi.purchasePayment(kbAccountId, paymentId, transactionId, paymentMethodId, amount, currency, null, context);
                case "refund":
                    return paymentApi.refundPayment(kbAccountId, paymentId, transactionId, paymentMethodId, amount, currency, null, context);
                default:
                    throw new IllegalArgumentException("Unsupported action: " + request.action);
            }
        } catch (Exception e) {
            _logger.error("Error processing payment test request", e);
            throw new RuntimeException("Payment test failed: " + e.getMessage());
        }
    }
}
