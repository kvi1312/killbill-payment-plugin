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

## _Read more_

- [Maven Central Dependency](https://central.sonatype.com/search)
