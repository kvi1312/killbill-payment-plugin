package org.killbill.billing.plugin.saleplugin;
import org.killbill.billing.invoice.plugin.api.InvoiceFormatterFactory;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.killbill.billing.plugin.helloworld.MetricsGeneratorExample;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Properties;

public class PluginActivator extends KillbillActivatorBase {
    public static final String PLUGIN_NAME = "TemperaturePlugin";

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
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
    }

    private void registerHandlers(){

    }

    private void registerPaymentPluginApi(){

    }

    private void registerHealthCheck(){

    }

}
