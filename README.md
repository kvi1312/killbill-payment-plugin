# **Sale Plugin for Kill Bill**

## **Overview**

The **Sale Plugin** is designed for an ice cream shop, offering dynamic percentage-based discounts based on the daily temperature.  
This plugin integrates with Kill Bill to apply conditional discounts, listen to billing events, and expose APIs for tenant-specific operations. 🐧

## **Functionality**

- **`PluginServlet`**: Exposes endpoints and retrieves tenant data.
- **`PluginListener`**: Listens for events from Kill Bill.
- **`PluginApi`**: Handles payment-related API operations.
- **`PluginActivator`**: Acts as the startup class, configuring and initializing the plugin.
- **`ConfigurationHandler`**: Reads and manages the plugin's configuration settings.

## **Important Notes**

- **Maven Bundle Plugin is required** – Without it, Tomcat will not recognize the plugin.
- **Ensure the packaging type is set to `bundle`** to comply with OSGi requirements.

## **Build and Deployment**

### **Rebuild JAR file**

```sh
mvn clean package
```

### **Generate the plugin**

```sh
kpm install_java_plugin sale --from-source-file target/sale-plugin-*-SNAPSHOT.jar --destination <path_to_plugin_directory>
```

- After generate JAR file, please following these steps [Deploy-by-hand-KPM](https://docs.killbill.io/latest/plugin_installation#_deploying_by_hand)

### Set up Account
1. Create a Kill Bill account for the customer (The following request uses the default Kill Bill API key and secret, change them if needed):

```
curl -v -X POST -u admin:password -H 'X-Killbill-ApiKey: bob' -H 'X-Killbill-ApiSecret: lazar' -H 'X-Killbill-CreatedBy: tutorial' -H 'Content-Type: application/json' -d '{ "currency": "USD" }' 'http://127.0.0.1:8080/1.0/kb/accounts'

```

This returns the Kill Bill `accountId` in the `Location` header.
For example, in the following sample response, `1f903207-8114-4110-8d4a-63ebbe72bf9b` is the account Id.

```
< Access-Control-Allow-Credentials: true
< Location: http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>
< Content-Type: application/json
```

2. Use the plugin `/checkout` API to create a redirect flow:

```
curl -X POST -u admin:password -H "X-Killbill-ApiKey: bob" -H "X-Killbill-ApiSecret: lazar" -H "X-Killbill-CreatedBy: tutorial" -H "Content-Type: application/json" -d "{\"accountId\": \"1f903207-8114-4110-8d4a-63ebbe72bf9b\", \"action\": \"purchase\", \"amount\": \"100.00\", \"currency\": \"USD\"}" "http://127.0.0.1:8080/plugins/sale-plugin/checkout?kbAccountId=<ACCOUNT_ID>"
```

## _Read more_

- [Maven Central Dependency](https://central.sonatype.com/search)
  
