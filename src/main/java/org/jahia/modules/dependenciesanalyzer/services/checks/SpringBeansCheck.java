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
            "ExternalProviderInitializerService",
            "HttpClientService",
            "JCRStoreService",
            "JahiaGroupManagerService",
            "JahiaSitesService",
            "JahiaTemplateManagerService",
            "JahiaUserManagerService",
            "MailService",
            "PublicationHelper",
            "SchedulerService",
            "SettingsBean",
            "SourceControlFactory",
            "authPipeline",
            "editmode",
            "ehCacheProvider",
            "ehCacheUtils",
            "imageService",
            "ImportExportService",
            "jahiaNotificationContext",
            "jahiaSitesService",
            "jcrPublicationService",
            "jcrSessionFactory",
            "jcrTemplate",
            "loggingService",
            "ModuleCacheProvider",
            "moduleSessionFactory",
            "org.jahia.services.seo.jcr.VanityUrlService",
            "org.jahia.services.tasks.TaskService",
            "permissionService",
            "RenderService",
            "scriptEngineUtils",
            "settingsBean",
            "studiomode",
            "urlResolverFactory",
            "UrlRewriteService",
            "visibilityService",
            "workflowService"
    );

    public SpringBeansCheck() {
        super("spring-beans", "Check that the Jahia dependencies match the ones expected by the Spring beans");
    }

    @Override
    public Map<String, String> buildOrigins() {
        final Map<String, String> origins = new HashMap<>();
        getActiveModules().forEach((module) -> {
            Enumeration<URL> urls = module.getBundle().findEntries("/", "*.class", true);
            final String moduleName = module.getId();

            if (urls != null) {
                while (urls.hasMoreElements()) {
                    final URL url = urls.nextElement();
                    final String className = url.getFile().replaceFirst("\\/", "").replaceFirst("\\.class", "").replaceAll("\\/", "\\.");
                    origins.put(className, moduleName);
                }
            }

            urls = module.getBundle().findEntries("/", "*.groovy", true);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    final URL url = urls.nextElement();
                    final String className = url.getFile().replaceFirst("\\/", "").replaceFirst("\\.groovy", "").replaceAll("\\/", "\\.");
                    origins.put(className, moduleName);
                }
            }

            urls = module.getBundle().findEntries("/META-INF/spring", "*.xml", true);
            if (urls != null) {
                while (urls.hasMoreElements()) {

                    try {
                        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        final DocumentBuilder builder = factory.newDocumentBuilder();
                        final URL url = urls.nextElement();

                        final Document document = builder.parse((InputStream) url.getContent());
                        final Element racine = document.getDocumentElement();
                        NodeList elements = racine.getElementsByTagName("lang:groovy");
                        for (int i = 0; i < elements.getLength(); i++) {
                            final Node bean = elements.item(i);
                            final Node nodeId = bean.getAttributes().getNamedItem("id");
                            if (nodeId != null) {
                                final String beanId = nodeId.getNodeValue();
                                origins.put(beanId, moduleName);
                            }
                        }

                        elements = racine.getElementsByTagName("bean");
                        for (int i = 0; i < elements.getLength(); i++) {
                            final Node bean = elements.item(i);
                            final Node nodeId = bean.getAttributes().getNamedItem("id");
                            if (nodeId != null) {
                                final String beanId = nodeId.getNodeValue();
                                origins.put(beanId, moduleName);
                            }
                        }

                        elements = racine.getElementsByTagName("osgi:list");
                        for (int i = 0; i < elements.getLength(); i++) {
                            final Node bean = elements.item(i);
                            final Node nodeId = bean.getAttributes().getNamedItem("id");
                            if (nodeId != null) {
                                final String beanId = nodeId.getNodeValue();
                                origins.put(beanId, moduleName);
                            }
                        }

                    } catch (ParserConfigurationException | SAXException | IOException ex) {
                        LOGGER.error("Impossible to read Spring XML file", ex);
                    }
                }

            }
        });
        return origins;
    }

    @Override

    public Map<String, Set<String>> getExpectedDependencies() {
        final Map<String, Set<String>> springBeans = new TreeMap<>();

        getActiveModules().forEach((module) -> {
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

                        NodeList properties = racine.getElementsByTagName("property");
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

                        properties = racine.getElementsByTagName("lang:groovy");
                        for (int i = 0; i < properties.getLength(); i++) {
                            final Node property = properties.item(i);
                            final Node refId = property.getAttributes().getNamedItem("script-source");
                            if (refId != null) {
                                final String beanId = refId.getNodeValue().replace("classpath:", "").replaceFirst("\\.groovy", "").replaceAll("\\/", "\\.");
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
                    } catch (ParserConfigurationException | SAXException | IOException ex) {
                        LOGGER.error("Impossible to read Spring XML file", ex);
                    }
                }
            }
        });
        return springBeans;
    }
}
