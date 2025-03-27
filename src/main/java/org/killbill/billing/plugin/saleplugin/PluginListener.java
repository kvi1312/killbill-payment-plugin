package org.killbill.billing.plugin.saleplugin;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.invoice.api.formatters.InvoiceItemFormatter;
import org.killbill.billing.invoice.plugin.api.InvoiceFormatterFactory;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.plugin.api.PluginTenantContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class PluginListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {
    private static Logger _logger = LoggerFactory.getLogger(PluginListener.class);

    private final OSGIKillbillAPI _killbillAPI;

    private final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> _invoiceFormatterTracker;

    private final Properties _configProperties;

    private static final String defaultLocale = "en_US";

    public PluginListener(OSGIKillbillAPI killbillAPI, ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker, Properties properties, OSGIKillbillAPI killbillAPI1, ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker1, Properties configProperties) {

        _killbillAPI = killbillAPI1;
        _invoiceFormatterTracker = invoiceFormatterTracker1;
        _configProperties = configProperties;
    }

    @Override
    public void handleKillbillEvent(ExtBusEvent killbillEvent) {
        _logger.info("Received event {} for object id {} of type {}",
                killbillEvent.getEventType(),
                killbillEvent.getObjectId(),
                killbillEvent.getObjectType());

        final TenantContext context = new PluginTenantContext(killbillEvent.getAccountId(), killbillEvent.getTenantId());
        switch (killbillEvent.getEventType()) {
            case ACCOUNT_CHANGE:
            case ACCOUNT_CREATION:
                try {
                    final Account account = _killbillAPI.getAccountUserApi().getAccountById(killbillEvent.getObjectId(), context);
                    _logger.info("Account information : {}", account);
                } catch (AccountApiException ex) {
                    _logger.error("Account information could not be retrieved", ex);
                }
                break;
            case INVOICE_CREATION:
                Account account = null;
                try {
                    account = _killbillAPI.getAccountUserApi().getAccountById(killbillEvent.getObjectId(), context);
                } catch (AccountApiException ex) {
                    _logger.error("[INVOICE_CREATION] Account information could not be retrieved", ex);
                }

                final List<Invoice> invoices = _killbillAPI.getInvoiceUserApi().getInvoicesByAccount(killbillEvent.getAccountId(), false, false, true, context);
                _logger.info("Invoices in sale-plugin {}: ", invoices.size());

                final String invoiceFormatterPluginName = _configProperties.getProperty("org.killbill.template.invoiceFormatterFactoryPluginName");

                if (invoiceFormatterPluginName == null || invoiceFormatterPluginName.isEmpty()) {
                    _logger.warn("Invoice formatter factory plugin name is empty. Set org.killbill.template.invoiceFormatterFactoryPluginName to configure it");
                    return;
                }

                final InvoiceFormatterFactory formatterFactory = (_invoiceFormatterTracker != null ? _invoiceFormatterTracker.getService() : null);
                Invoice invoice = invoices.get(0);
                InvoiceFormatter invoiceFormatter = formatterFactory.createInvoiceFormatter(defaultLocale, null, invoice, Locale.forLanguageTag(account.getLocale()), _killbillAPI.getCurrencyConversionApi(), null, null);

                List<InvoiceItem> items = invoiceFormatter.getInvoiceItems();
                _logger.info("sale-plugin got items:{}", items.size());
                for (InvoiceItem item : items) {
                    final InvoiceItemFormatter invoiceItemFormatter = (InvoiceItemFormatter) item;
                    final String formattedEndDate = invoiceItemFormatter.getFormattedEndDate();
                    _logger.info("sale-plugin formattedEndDate:{}", formattedEndDate);
                }
            default:
                break;
        }
    }
}
