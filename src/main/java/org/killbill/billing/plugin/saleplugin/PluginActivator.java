package org.killbill.billing.plugin.saleplugin;
import org.killbill.billing.invoice.plugin.api.InvoiceFormatterFactory;
import org.killbill.billing.invoice.plugin.api.InvoicePluginApi;
import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.killbill.billing.plugin.helloworld.MetricsGeneratorExample;
import org.killbill.billing.plugin.saleplugin.Api.InvoiceApi;
import org.killbill.billing.plugin.saleplugin.Api.PaymentApi;
import org.killbill.billing.plugin.saleplugin.Services.PluginConfigurationHandler;
import org.killbill.billing.plugin.saleplugin.Services.PluginHealthCheck;
import org.killbill.billing.plugin.saleplugin.Services.PluginListener;
import org.killbill.billing.plugin.saleplugin.Services.PluginServlet;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;

import java.util.Hashtable;
import java.util.Properties;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIFrameworkEventHandler;

public class PluginActivator extends KillbillActivatorBase {
    public static final String PLUGIN_NAME = "sale-plugin";

    private PluginConfigurationHandler _configurationHandler;
    private OSGIKillbillEventDispatcher.OSGIKillbillEventHandler _killbillEventHandler;
    private MetricsGeneratorExample _metricsGenerator;

    private ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> _invoiceFormatterTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        final String region = PluginEnvironmentConfig.getRegion(configProperties.getProperties());

        // Register an event listener for plugin configuration
        _configurationHandler = new PluginConfigurationHandler(region, PLUGIN_NAME, killbillAPI);
        final Properties globalConfiguration = _configurationHandler.createConfigurable(configProperties.getProperties());
        _configurationHandler.setDefaultConfigurable(globalConfiguration);

        // open service tracker for
        _invoiceFormatterTracker = new ServiceTracker<>(context, InvoiceFormatterFactory.class, null);
        _invoiceFormatterTracker.open();

        _killbillEventHandler = new PluginListener(killbillAPI, _invoiceFormatterTracker, configProperties.getProperties());

        final PaymentPluginApi paymentPluginApi = new PaymentApi();
        registerPaymentPluginApi(context, paymentPluginApi);

        _metricsGenerator = new MetricsGeneratorExample(metricRegistry);
        _metricsGenerator.start();

        final Healthcheck healthcheck = new PluginHealthCheck();
        registerHealthCheck(context, healthcheck);

        final InvoicePluginApi invoicePluginApi = new InvoiceApi(killbillAPI, configProperties, null);
        registerInvoicePluginApi(context, invoicePluginApi);

        final PluginApp app = new PluginAppBuilder(PLUGIN_NAME, killbillAPI, dataSource, super.clock, configProperties)
                                                        .withRouteClass(PluginServlet.class)
                                                        .withRouteClass(PluginHealthCheck.class)
                                                        .withService(healthcheck).build();
        final HttpServlet httpServlet = PluginApp.createServlet(app);
        registerServlet(context, httpServlet);

        registerHandlers();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        _metricsGenerator.stop();
        super.stop(context);
    }

    private void registerHandlers(){
        final PluginConfigurationEventHandler configHandler = new PluginConfigurationEventHandler(_configurationHandler);
        dispatcher.registerEventHandlers(configHandler, (OSGIFrameworkEventHandler) () -> dispatcher.registerEventHandlers(_killbillEventHandler));
    }

    private void registerPaymentPluginApi(BundleContext context, PaymentPluginApi paymentPluginApi ) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, paymentPluginApi, props);
    }

    private void registerHealthCheck(BundleContext context, Healthcheck healthcheck) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Healthcheck.class, healthcheck, props);
    }

    private void registerServlet(BundleContext context, HttpServlet httpServlet){
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, httpServlet, props);
    }

    private void registerInvoicePluginApi(BundleContext context, InvoicePluginApi invoicePluginApi) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, InvoicePluginApi.class, invoicePluginApi, props);
    }

}
