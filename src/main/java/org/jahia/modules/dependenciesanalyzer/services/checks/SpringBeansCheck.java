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
public class SpringBeansCheck extends AbstractDependenciesAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBeansCheck.class);
    private static final List<String> EXCLUDED_BEAN = Arrays.asList(
            "ChannelService",
            "ContentManagerHelper",
            "DefaulJCRStoreProvider",
            "DocumentConverterService",
            "editmode",
            "ExternalProviderInitializerService",
            "HttpClientService",
            "JCRStoreService",
            "JahiaGroupManagerService",
            "JahiaSitesService",
            "JahiaTemplateManagerService",
            "JahiaUserManagerService",
            "MailService",
            "PublicationHelper",
            "SettingsBean",
            "SourceControlFactory",
            "authPipeline",
            "ehCacheProvider",
            "imageService",
            "jahiaNotificationContext",
            "jcrPublicationService",
            "jcrSessionFactory",
            "jcrTemplate",
            "loggingService",
            "moduleSessionFactory",
            "org.jahia.services.seo.jcr.VanityUrlService",
            "org.jahia.services.tasks.TaskService",
            "permissionService",
            "SchedulerService",
            "scriptEngineUtils",
            "settingsBean",
            "studiomode",
            "urlResolverFactory",
            "visibilityService",
            "workflowService"
    );

    public SpringBeansCheck() {
        super("spring-beans");
    }

    @Override
    public Map<String, String> buildOrigins() {
        final Map<String, String> origins = new HashMap<>();
        for (JahiaTemplatesPackage module : getActiveModules()) {
            final Enumeration<URL> urls = module.getBundle().findEntries("/", "*.class", true);
            if (urls != null) {
                final String moduleName = module.getId();
                while (urls.hasMoreElements()) {
                    final URL url = urls.nextElement();
                    final String className = url.getFile().replaceFirst("\\/", "").replaceFirst("\\.class", "").replaceAll("\\/", "\\.");
                    origins.put(className, moduleName);
                }
            }
        }
        return origins;
    }

    @Override
    public Map<String, Set<String>> getExpectedDependencies() {
        final Map<String, Set<String>> springBeans = new TreeMap<>();

        for (JahiaTemplatesPackage module : getActiveModules()) {
            final Enumeration<URL> urls = module.getBundle().findEntries("/META-INF/spring", "*.xml", true);
            final String moduleName = module.getId();
            if (urls != null) {
                while (urls.hasMoreElements()) {

                    try {
                        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        final DocumentBuilder builder = factory.newDocumentBuilder();
                        final URL url = urls.nextElement();

                        final Document document = builder.parse((InputStream) url.getContent());
                        final Element racine = document.getDocumentElement();
                        final NodeList elements = racine.getElementsByTagName("bean");
                        for (int i = 0; i < elements.getLength(); i++) {
                            final Node bean = elements.item(i);
                            final Node nodeId = bean.getAttributes().getNamedItem("ref");
                            if (nodeId != null) {
                                final String beanId = nodeId.getNodeValue();
                                if (!EXCLUDED_BEAN.contains(beanId)) {
                                    final Set<String> dependencies;
                                    if (springBeans.containsKey(moduleName)) {
                                        dependencies = springBeans.get(moduleName);
                                    } else {
                                        dependencies = new TreeSet<>();
                                        springBeans.put(moduleName, dependencies);
                                    }
                                    dependencies.add(beanId);
                                }
                            }
                        }

                        final NodeList properties = racine.getElementsByTagName("property");
                        for (int i = 0; i < properties.getLength(); i++) {
                            final Node property = properties.item(i);
                            final Node refId = property.getAttributes().getNamedItem("ref");
                            if (refId != null) {
                                final String beanId = refId.getNodeValue();
                                if (!EXCLUDED_BEAN.contains(beanId)) {
                                    final Set<String> dependencies;
                                    if (springBeans.containsKey(moduleName)) {
                                        dependencies = springBeans.get(moduleName);
                                    } else {
                                        dependencies = new TreeSet<>();
                                        springBeans.put(moduleName, dependencies);
                                    }
                                    dependencies.add(beanId);
                                }
                            }
                        }
                    } catch (ParserConfigurationException | SAXException | IOException ex) {
                        LOGGER.error("Impossible to read Spring XML file", ex);
                    }
                }
            }
        }
        return springBeans;
    }
}
