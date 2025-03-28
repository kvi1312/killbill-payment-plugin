package org.killbill.billing.plugin.saleplugin.Api;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.plugin.api.*;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.invoice.PluginInvoiceItem;
import org.killbill.billing.plugin.api.invoice.PluginInvoicePluginApi;
import org.killbill.clock.Clock;

import java.math.BigDecimal;
import java.util.*;

public class InvoiceApi extends PluginInvoicePluginApi implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    public InvoiceApi(OSGIKillbillAPI killbillAPI, OSGIConfigPropertiesService configProperties, final Clock clock) {
        super(killbillAPI, configProperties, clock);
    }

    @Override
    public AdditionalItemsResult getAdditionalInvoiceItems(Invoice newInvoice, boolean dryRun, Iterable<PluginProperty> properties, InvoiceContext context) {
        final UUID accountId = newInvoice.getAccountId();
        final Account account = getAccount(accountId, context);
        final Set<Invoice> allInvoices = getAllInvoicesOfAccount(account, newInvoice, context);
        final List<InvoiceItem> additionalItems = new LinkedList<InvoiceItem>();
        // Creating tax item for first Item of new Invoice
        final List<InvoiceItem> newInvoiceItems = newInvoice.getInvoiceItems();
        final InvoiceItem newInvoiceItem = newInvoiceItems.get(0);
        BigDecimal charge = new BigDecimal("80");
        final InvoiceItem taxItem = PluginInvoiceItem.createTaxItem(newInvoiceItem, newInvoiceItem.getInvoiceId(),
                newInvoice.getInvoiceDate(), null, charge, "Tax Item");
        additionalItems.add(taxItem);

        // Creating External Charge for first Item of new Invoice
        final InvoiceItem externalItem = PluginInvoiceItem.create(newInvoiceItem, newInvoiceItem.getInvoiceId(),
                newInvoice.getInvoiceDate(), null, charge, "External Item", InvoiceItemType.EXTERNAL_CHARGE);
        additionalItems.add(externalItem);

        // Adding adjustment invoice item to the first historical invoice, if it does not have the adjustment item
        for (final Invoice invoice : allInvoices) {
            if (!invoice.getId().equals(newInvoice.getId())) {
                final List<InvoiceItem> invoiceItems = invoice.getInvoiceItems();
                // Check for if any adjustment item exists for Historical Invoice
                if (checkforAdjustmentItem(invoiceItems)) {
                    break;
                }
                for (final InvoiceItem item : invoiceItems) {
                    charge = new BigDecimal("-30");
                    final InvoiceItem adjItem = PluginInvoiceItem.createAdjustmentItem(item, item.getInvoiceId(),
                            newInvoice.getInvoiceDate(), newInvoice.getInvoiceDate(), charge, "Adjustment Item");
                    additionalItems.add(adjItem);
                    break;
                }
                break;
            }
        }

        return new AdditionalItemsResult() {
            @Override
            public List<InvoiceItem> getAdditionalItems() {
                return additionalItems;
            }

            @Override
            public Iterable<PluginProperty> getAdjustedPluginProperties() {
                return null;
            }
        };

    }

    private boolean checkforAdjustmentItem(List<InvoiceItem> invoiceItems) {
        boolean adjustmentItemPresent = false;
        for (final InvoiceItem invoiceItem : invoiceItems) {
            if (invoiceItem.getInvoiceItemType().equals(InvoiceItemType.ITEM_ADJ)) {
                adjustmentItemPresent = true;
                break;
            }
        }
        return adjustmentItemPresent;
    }


    private Set<Invoice> getAllInvoicesOfAccount(Account account, Invoice newInvoice, InvoiceContext context) {
        final Set<Invoice> invoices= new HashSet<Invoice>();
        invoices.addAll(getInvoicesByAccountId(account.getId(), context));
        invoices.add(newInvoice);
        return invoices;
    }

    protected boolean isTaxItem(final InvoiceItem invoiceItem) {
        return InvoiceItemType.TAX.equals(invoiceItem.getInvoiceItemType());
    }

    @Override
    public void handleKillbillEvent(ExtBusEvent killbillEvent) {

    }
}
