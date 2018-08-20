package org.jahia.modules.dependenciesanalyzer.services.checks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
public class MavenCheck extends AbstractDependenciesAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenCheck.class);

    public MavenCheck() {
        super("maven");
    }

    @Override
    public Map<String, String> buildOrigins() {
        return new HashMap<>();
    }

    @Override

    public Map<String, Set<String>> getExpectedDependencies() {
        final Map<String, Set<String>> mavenDependencies = new TreeMap<>();
        final Set<String> modules = new HashSet<>();
        getActiveModules().forEach((module) -> {
            modules.add(module.getId());
        });

        getActiveModules().forEach((module) -> {
            final Enumeration<URL> urls = module.getBundle().findEntries("/", "pom.xml", true);
            final String moduleName = module.getId();
            if (urls != null) {
                while (urls.hasMoreElements()) {

                    try {
                        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        final DocumentBuilder builder = factory.newDocumentBuilder();
                        final URL url = urls.nextElement();

                        final Document document = builder.parse((InputStream) url.getContent());
                        final Element racine = document.getDocumentElement();
                        final NodeList elements = racine.getElementsByTagName("dependency");
                        for (int i = 0; i < elements.getLength(); i++) {
                            final Node bean = elements.item(i);
                            final NodeList childNodes = bean.getChildNodes();
                            String artifact = null;
                            String scope = null;
                            for (int childIdx = 0; childIdx < childNodes.getLength(); childIdx++) {
                                final Node child = childNodes.item(childIdx);

                                if (child.getNodeType() == Node.ELEMENT_NODE) {
                                    final String childName = child.getNodeName();
                                    if (childName.equals("artifactId")) {
                                        artifact = child.getTextContent();
                                    } else if (childName.equals("scope")) {
                                        scope = child.getTextContent();
                                    }
                                }

                            }

                            if (artifact != null && modules.contains(artifact) && (scope == null || !scope.equals("provided"))) {
                                final Set<String> dependencies;
                                if (mavenDependencies.containsKey(moduleName)) {
                                    dependencies = mavenDependencies.get(moduleName);
                                } else {
                                    dependencies = new TreeSet<>();
                                    mavenDependencies.put(moduleName, dependencies);
                                }
                                dependencies.add(artifact);
                            }
                        }

                    } catch (ParserConfigurationException | SAXException | IOException ex) {
                        LOGGER.error("Impossible to read Spring XML file", ex);
                    }
                }
            }
        });
        return mavenDependencies;
    }
}
