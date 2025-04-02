package org.killbill.billing.plugin.saleplugin.Api;

import org.jooq.tools.json.JSONObject;
import org.jooq.tools.json.JSONParser;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.*;
import org.killbill.billing.plugin.saleplugin.Extensions.ConfigLoader;
import org.killbill.billing.plugin.saleplugin.Extensions.SalePluginPaymentTransactionInfoPlugin;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentApi implements PaymentPluginApi {
    private static final Logger _logger = LoggerFactory.getLogger(PaymentApi.class);
    private OSGIKillbillAPI _killbillAPI;
    private Clock _clock;
    private static final String WEATHER_API_KEY = ConfigLoader.getProperty("weather.api.key");
    private static final String WEATHER_API_URL = ConfigLoader.getProperty("weather.api.url");
    private static final String DEFAULT_CITY = ConfigLoader.getProperty("weather.api.default.city");

    private static final double MAX_DISCOUNT_PERCENT = 50.0;

    public PaymentApi(OSGIKillbillAPI killbillAPI, Clock clock) {
        _killbillAPI = killbillAPI;
        _clock = clock;
    }

    @Override
    public SalePluginPaymentTransactionInfoPlugin authorizePayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId,
                                                         UUID kbPaymentMethodId, BigDecimal amount, Currency currency,
                                                         Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        _logger.info("[authorizePayment] Processing authorize payment for account ID: {}", kbAccountId);
        double temperature = getCurrentTemperature();
        double discountPercent = calculateDiscountPercent(temperature);
        BigDecimal discountedAmount = new BigDecimal(discountPercent);

        _logger.info("[authorizePayment] Original amount: {}, Temperature: {}°C, Discount: {}%, Final amount: {} 🔥", amount, temperature, discountPercent, discountedAmount);
        List<PluginProperty> pluginProperties = new ArrayList<>();
        pluginProperties.add(new PluginProperty("temperature", String.valueOf(temperature), false));
        pluginProperties.add(new PluginProperty("discount_percent", String.valueOf(discountPercent), false));
        pluginProperties.add(new PluginProperty("original_amount", amount.toString(), false));

        if (properties != null) {
            for (PluginProperty property : properties) {
                pluginProperties.add(property);
            }
        }
        return new SalePluginPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                TransactionType.AUTHORIZE,
                discountedAmount,
                currency,
                PaymentPluginStatus.PROCESSED,
                "Discount based on temperature applied successfully", // gatewayError
                "Temperature Discount: " + discountPercent + "%",     // gatewayErrorCode
                null,                                                 // firstPaymentReferenceId
                null,                                                 // secondPaymentReferenceId
                context.getCreatedDate(),                             // createdDate
                context.getCreatedDate(),                             // effectiveDate
                pluginProperties
        );
    }

    @Override
    public SalePluginPaymentTransactionInfoPlugin capturePayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId,
                                                       UUID kbPaymentMethodId, BigDecimal amount, Currency currency,
                                                       Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        _logger.info("[capturePayment] Processing capture for account ID: {}", kbAccountId);
        List<PluginProperty> pluginProperties = new ArrayList<>();
        if (properties != null) {
            for (PluginProperty property : properties) {
                pluginProperties.add(property);
            }
        }

        return new SalePluginPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                TransactionType.CAPTURE,
                amount,
                currency,
                PaymentPluginStatus.PROCESSED,
                "Payment captured successfully with temperature discount",
                null,
                null,
                null,
                context.getCreatedDate(),
                context.getCreatedDate(),
                pluginProperties
        );
    }

    @Override
    public SalePluginPaymentTransactionInfoPlugin purchasePayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId,
                                                        UUID kbPaymentMethodId, BigDecimal amount, Currency currency,
                                                        Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        _logger.info("[purchasePayment] Processing purchase for account ID: {}", kbAccountId);

        double temperature = getCurrentTemperature();

        double discountPercent = calculateDiscountPercent(temperature);

        BigDecimal discountedAmount = applyDiscount(amount, discountPercent);

        _logger.info("Original amount: {}, Temperature: {}°C, Discount: {}%, Final amount: {}",
                amount, temperature, discountPercent, discountedAmount);

        List<PluginProperty> pluginProperties = new ArrayList<>();
        pluginProperties.add(new PluginProperty("temperature", String.valueOf(temperature), false));
        pluginProperties.add(new PluginProperty("discount_percent", String.valueOf(discountPercent), false));
        pluginProperties.add(new PluginProperty("original_amount", amount.toString(), false));

        if (properties != null) {
            for (PluginProperty property : properties) {
                pluginProperties.add(property);
            }
        }

        return new SalePluginPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                TransactionType.PURCHASE,
                discountedAmount,
                currency,
                PaymentPluginStatus.PROCESSED,
                "Purchase processed with temperature discount",
                "Discount: " + discountPercent, // gatewayErrorCode
                null,                                              // firstPaymentReferenceId
                null,                                              // secondPaymentReferenceId
                context.getCreatedDate(),                          // createdDate
                context.getCreatedDate(),                          // effectiveDate
                pluginProperties
        );
    }

    @Override
    public SalePluginPaymentTransactionInfoPlugin voidPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId,
                                                    UUID kbPaymentMethodId, Iterable<PluginProperty> properties,
                                                    CallContext context) throws PaymentPluginApiException {
        _logger.info("[voidPayment] Processing void for account ID: {}", kbAccountId);

        List<PluginProperty> pluginProperties = new ArrayList<>();

        if (properties != null) {
            for (PluginProperty property : properties) {
                pluginProperties.add(property);
            }
        }

        return new SalePluginPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                TransactionType.VOID,
                null,
                null,
                PaymentPluginStatus.PROCESSED,
                "Payment voided successfully",
                null,
                null,
                null,
                context.getCreatedDate(),
                context.getCreatedDate(),
                pluginProperties
        );
    }

    @Override
    public SalePluginPaymentTransactionInfoPlugin creditPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId,
                                                      UUID kbPaymentMethodId, BigDecimal amount, Currency currency,
                                                      Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        _logger.info("[creditPayment] Processing credit for account ID: {}", kbAccountId);

        List<PluginProperty> pluginProperties = new ArrayList<>();

        if (properties != null) {
            for (PluginProperty property : properties) {
                pluginProperties.add(property);
            }
        }

        return new SalePluginPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                TransactionType.CREDIT,
                amount,
                currency,
                PaymentPluginStatus.PROCESSED,
                "Credit processed successfully",
                null,
                null,
                null,
                context.getCreatedDate(),
                context.getCreatedDate(),
                pluginProperties
        );
    }

    @Override
    public SalePluginPaymentTransactionInfoPlugin refundPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId,
                                                      UUID kbPaymentMethodId, BigDecimal amount, Currency currency,
                                                      Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        _logger.info("[refundPayment] Processing refund for account ID: {}", kbAccountId);

        List<PluginProperty> pluginProperties = new ArrayList<>();

        if (properties != null) {
            for (PluginProperty property : properties) {
                pluginProperties.add(property);
            }
        }

        return new SalePluginPaymentTransactionInfoPlugin(
                kbPaymentId,
                kbTransactionId,
                TransactionType.REFUND,
                amount,
                currency,
                PaymentPluginStatus.PROCESSED,
                "Refund processed successfully",
                null,
                null,
                null,
                context.getCreatedDate(),
                context.getCreatedDate(),
                pluginProperties
        );
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(UUID kbAccountId, UUID kbPaymentId,
                                                             Iterable<PluginProperty> properties, TenantContext context)
            throws PaymentPluginApiException {
        _logger.info("[getPaymentInfo] Getting payment info for account ID: {}", kbAccountId);
        return List.of();
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(String searchKey, Long offset, Long limit,
                                                                   Iterable<PluginProperty> properties,
                                                                   TenantContext context)
            throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void addPaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, PaymentMethodPlugin paymentMethodProps,
                                 boolean setDefault, Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        _logger.info("[addPaymentMethod] Kill Bill Account ID: {}", kbAccountId);
    }

    @Override
    public void deletePaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties,
                                    CallContext context) throws PaymentPluginApiException {
        _logger.info("[deletePaymentMethod] Deleting payment method for account ID: {}", kbAccountId);
    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(UUID kbAccountId, UUID kbPaymentMethodId,
                                                      Iterable<PluginProperty> properties, TenantContext context)
            throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void setDefaultPaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties,
                                        CallContext context) throws PaymentPluginApiException {
        _logger.info("[setDefaultPaymentMethod] Setting default payment method for account ID: {}", kbAccountId);
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(UUID kbAccountId, boolean refreshFromGateway,
                                                           Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        return List.of();
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(String searchKey, Long offset, Long limit,
                                                                Iterable<PluginProperty> properties,
                                                                TenantContext context)
            throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void resetPaymentMethods(UUID kbAccountId, List<PaymentMethodInfoPlugin> paymentMethods,
                                    Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        _logger.info("[resetPaymentMethods] Resetting payment methods for account ID: {}", kbAccountId);
    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(UUID kbAccountId, Iterable<PluginProperty> customFields,
                                                               Iterable<PluginProperty> properties, CallContext context)
            throws PaymentPluginApiException {
        _logger.info("[buildFormDescriptor] Kill Bill Account ID: {}", kbAccountId);
        return null;
    }

    @Override
    public GatewayNotification processNotification(String notification, Iterable<PluginProperty> properties,
                                                   CallContext context) throws PaymentPluginApiException {
        return null;
    }

    private double getCurrentTemperature() {
        String apiUrl = WEATHER_API_URL + "?q=" + DEFAULT_CITY + "&appid=" + WEATHER_API_KEY + "&units=metric";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(response.toString());
            JSONObject main = (JSONObject) jsonObject.get("main");
            Number tempeValue = (Number) main.get("temp");
            double temperature = tempeValue.doubleValue();

            _logger.info("Current temperature in {}: {} °C", DEFAULT_CITY, temperature);
            return temperature;
        } catch (Exception e) {
            _logger.error("Error getting temperature: {}", e.getMessage(), e);
            return 0.00;
        }
    }

    private double calculateDiscountPercent(double temperature) {
        double discountPercent = temperature;

        if (discountPercent > MAX_DISCOUNT_PERCENT) {
            discountPercent = MAX_DISCOUNT_PERCENT;
        }

        if (discountPercent < 0) {
            discountPercent = 0;
        }

        _logger.info("Applied discount based on temperature: {}%", discountPercent);
        return discountPercent;
    }

    private BigDecimal applyDiscount(BigDecimal amount, double discountPercent) {
        BigDecimal discountFactor = BigDecimal.ONE.subtract(BigDecimal.valueOf(discountPercent / 100.0));
        return amount.multiply(discountFactor).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
