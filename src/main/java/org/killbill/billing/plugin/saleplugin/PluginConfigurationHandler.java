package org.killbill.billing.plugin.saleplugin;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class PluginConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<Properties> {
    private static final Logger _logger = LoggerFactory.getLogger(PluginConfigurationHandler.class);
    private final String _region;
    public PluginConfigurationHandler(final String region, final String pluginName, final OSGIKillbillAPI osgiKillbillAPI) {
        super(pluginName, osgiKillbillAPI);
        _region = region;
    }

    @Override
    protected Properties createConfigurable(final Properties properties) {
        _logger.info("New properties for region {}: {}", _region, properties);
        return properties;
    }
}
