Function
- PluginServlet : Expose endpoint and getting tenant data
- PluginListener : Listening event from kill bill
- PluginApi : Handle payment api
- PluginActivator : Start up class, set up configuration
- ConfigurationHandler : Read and handle plugin's configuration

NOTE
- @Local : an annotation of jooby fw, use for marking a var that injected by the context of request -> that mean the value of var can change on each request
- @Named : an annotation of google juice (DI lib), use to declare what is using for inject into a @Local var

CMD
- Rebuild jar file on target mvn clean package
- kpm install_java_plugin sale --from-source-file target/sale-plugin-*-SNAPSHOT.jar --destination /var/tmp/bundles