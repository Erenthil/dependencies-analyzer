package org.jahia.modules.dependenciesanalyzer.services.checks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalysis;
import org.jahia.modules.dependenciesanalyzer.services.impl.AbstractDependenciesAnalysis;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component(service = DependenciesAnalysis.class, immediate = true)
public class OsgiServicesCheck extends AbstractDependenciesAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsgiServicesCheck.class);
    private static final List<String> EXCLUDED_OSGI_INTERFACE = Arrays.asList("org.osgi.service.cm.ManagedService", "org.osgi.service.cm.ManagedServiceFactory");

    public OsgiServicesCheck() {
        super("osgi-services");
    }

    @Override
    public Map<String, String> buildOrigins() {
        final Map<String, String> origins = new HashMap<>();
        getActiveModules().forEach((module) -> {
            final Enumeration<URL> urls = module.getBundle().findEntries("/", "*.class", true);
            if (urls != null) {
                final String moduleName = module.getId();
                while (urls.hasMoreElements()) {
                    final URL url = urls.nextElement();
                    final String className = url.getFile().replaceFirst("\\/", "").replaceFirst("\\.class", "").replaceAll("\\/", "\\.");
                    origins.put(className, moduleName);
                }
            }
        });
        return origins;
    }

    @Override
    public Map<String, Set<String>> getExpectedDependencies() {
        final Map<String, Set<String>> osgiServices = new TreeMap<>();

        getActiveModules().forEach((module) -> {
            final Enumeration<URL> urls = module.getBundle().findEntries("/META-INF/spring", "*.xml", true);
            if (urls != null) {
                final String moduleName = module.getId();
                while (urls.hasMoreElements()) {

                    try {
                        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        final DocumentBuilder builder = factory.newDocumentBuilder();
                        final URL url = urls.nextElement();

                        final Document document = builder.parse((InputStream) url.getContent());
                        final Element racine = document.getDocumentElement();
                        final NodeList elements = racine.getElementsByTagName("osgi:service");
                        for (int i = 0; i < elements.getLength(); i++) {
                            final Node osgiService = elements.item(i);
                            Node interfaceNode = osgiService.getAttributes().getNamedItem("interface");
                            if (interfaceNode != null) {
                                final String interfaceName = interfaceNode.getNodeValue();
                                if (!EXCLUDED_OSGI_INTERFACE.contains(interfaceName)) {
                                    final Set<String> dependencies;

                                    if (osgiServices.containsKey(moduleName)) {
                                        dependencies = osgiServices.get(moduleName);
                                    } else {
                                        dependencies = new TreeSet<>();
                                        osgiServices.put(moduleName, dependencies);
                                    }
                                    dependencies.add(interfaceName);
                                }
                            }
                        }
                    } catch (ParserConfigurationException | SAXException | IOException ex) {
                        LOGGER.error("Impossible to read Spring XML file", ex);
                    }
                }
            }
        });
        return osgiServices;
    }
}
