package org.jahia.modules.dependenciesanalyzer.services;

import io.github.livingdocumentation.dotdiagram.DotGraph;
import io.github.livingdocumentation.dotdiagram.DotGraph.Digraph;
import io.github.livingdocumentation.dotdiagram.DotStyles;
import io.github.livingdocumentation.dotdiagram.GraphvizDotWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.jcr.RepositoryException;
import org.jahia.api.Constants;
import org.jahia.bin.Jahia;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalysis;
import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalyzerService;
import org.jahia.modules.dependenciesanalyzer.services.impl.AbstractDependenciesAnalysis;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;

@Component(name = "org.jahia.modules.dependenciesanalyzer.service", service = DependenciesAnalyzerService.class, property = {
    org.osgi.framework.Constants.SERVICE_PID + "=org.jahia.modules.dependenciesanalyzer.service",
    org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=Dependencies analyzer service",
    org.osgi.framework.Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME}, immediate = true)
public class DependenciesAnalyzerServiceImpl implements DependenciesAnalyzerService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DependenciesAnalyzerServiceImpl.class);
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir") + "/";
    private static final String OUTPUT_EXTENSION = "jpg";
    public static final String GROUP_ID_JAHIA_MODULES = "org.jahia.modules";
    private final List<DependenciesAnalysis> dependenciesAnalyses = new ArrayList<>();
    private long dependenciesAnalysisIdGenerator = 0;

    @Activate
    public void start() {
        LOGGER.info("Dependencies analyzer service started");
    }

    @Deactivate
    public void stop() {
        LOGGER.info("Dependencies analyzer service stopped");
    }

    @Reference(service = DependenciesAnalysis.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "unregisterDependenciesAnalysis")
    public void registerDependenciesAnalysis(DependenciesAnalysis dependenciesAnalysis) {
        dependenciesAnalysis.setId(getNextDependenciesAnalysisID());
        dependenciesAnalyses.add(dependenciesAnalysis);
        LOGGER.info(String.format("Registered %s in the dependenciesAnalyzer service, number of analyses: %s, service: %s, CL: %s", dependenciesAnalysis, dependenciesAnalyses.size(), this, this.getClass().getClassLoader()));
    }

    public void unregisterDependenciesAnalysis(DependenciesAnalysis dependenciesAnalysis) {
        final boolean success = dependenciesAnalyses.remove(dependenciesAnalysis);
        if (success) {
            LOGGER.info(String.format("Unregistered %s in the dependenciesAnalyzer service, number of analyses: %s", dependenciesAnalysis, dependenciesAnalyses.size()));
        } else {
            LOGGER.error(String.format("Failed to unregister %s in the dependenciesAnalyzer service, number of analyses: %s", dependenciesAnalysis, dependenciesAnalyses.size()));
        }
    }

    private synchronized long getNextDependenciesAnalysisID() {
        return ++dependenciesAnalysisIdGenerator;
    }

    @Override
    public List<String> printDependenciesAnalysesList() {
        final int nbAnalyses = dependenciesAnalyses.size();
        final List<String> lines = new ArrayList<>(nbAnalyses + 1);

        final List<DependenciesResults> currentDependenciesResults = new ArrayList<>();
        final Map<String, Set<String>> currentDependencies = getCurrentDependencies();
        setCurrentDependenciesResults(currentDependencies, currentDependenciesResults);

        final List<DependenciesResults> missingDependenciesResults = new ArrayList<>();
        dependenciesAnalyses.stream().map(dependenciesAnalysis -> {
            return dependenciesAnalysis;
        }).map(dependenciesAnalysis -> {
            dependenciesAnalysis.setExpectedDependencies();
            return dependenciesAnalysis;
        }).forEachOrdered(dependenciesAnalysis -> {
            dependenciesAnalysis.calculateMissingDependencies(currentDependencies, missingDependenciesResults);
        });

        missingDependenciesResults.forEach(dependenciesResults -> {
            final String module = dependenciesResults.getModule();
            final String type = dependenciesResults.getType();
            final String description = dependenciesResults.getDescription();
            dependenciesResults.getDependencies().forEach(dependency -> {
                lines.add(String.format("%s;%s;%s;%s", module, type, dependency, description));
            });
        });
        return lines;
    }

    @Override
    public void writeGraph(boolean skipJahiaModule) {

        final List<DependenciesResults> currentDependenciesResults = new ArrayList<>();
        final Map<String, Set<String>> currentDependencies = getCurrentDependencies();
        setCurrentDependenciesResults(currentDependencies, currentDependenciesResults);
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");
        final String storageFolder = dateFormat.format(new Date());

        dependenciesAnalyses.forEach(dependenciesAnalysis -> {
            final String analysisName = dependenciesAnalysis.getName();
            final List<DependenciesResults> missingDependenciesResults = new ArrayList<>();
            dependenciesAnalysis.setExpectedDependencies();
            dependenciesAnalysis.calculateMissingDependencies(currentDependencies, missingDependenciesResults);

            final DotGraph graph = new DotGraph("Dependency analysis: " + analysisName);
            final Digraph digraph = graph.getDigraph();

            // Display in the graph the current dependencies
            currentDependenciesResults.stream().filter(results -> (!(skipJahiaModule && results.isJahiaModule()))).forEachOrdered(results -> {
                final String module = results.getModule();
                final Set<String> pomDependencies = results.getDependencies();
                updateGraph(pomDependencies, digraph, module, "arrowType=normal color=black");
            });

            // Display in the graph the missing dependencies
            missingDependenciesResults.forEach(results -> {
                final String module = results.getModule();
                final Set<String> pomDependencies = results.getDependencies();
                updateGraph(pomDependencies, digraph, module, "arrowType=normal color=red style=bold");
            });

            // Create the graph
            InputStream graphStream = null;
            final String dotName = analysisName + ".dot";
            final String graphName = analysisName + "." + OUTPUT_EXTENSION;
            try {
                final String command = String.format("%s -T %s -o %s %s", "dot", OUTPUT_EXTENSION, TMP_DIR + graphName, TMP_DIR + dotName);
                final GraphvizDotWriter dotWriter = new GraphvizDotWriter(TMP_DIR, "dot", OUTPUT_EXTENSION, command);

                dotWriter.write(analysisName, graph.render());
                dotWriter.render(analysisName);

                final JCRNodeWrapper dependenciesAnalyzerNode = mkdirs("/sites/systemsite/files/dependencies-analyzer/" + storageFolder);
                final File graphFile = new File(TMP_DIR + graphName);
                graphStream = new FileInputStream(graphFile);
                dependenciesAnalyzerNode.uploadFile(graphName, graphStream, "image/jpg");
                dependenciesAnalyzerNode.saveSession();
            } catch (IOException | InterruptedException | RepositoryException ex) {
                LOGGER.error("Impossible to create graph", ex);
            } finally {
                if (graphStream != null) {
                    try {
                        new File(TMP_DIR + dotName).delete();
                        new File(TMP_DIR + graphName).delete();
                        graphStream.close();
                    } catch (IOException ex) {
                    }
                }
            }
        });

    }

    private static void updateGraph(Set<String> dependencies, Digraph digraph, String module, String style) {
        if (dependencies.size() > 0) {
            final JahiaTemplateManagerService jahiaTemplateManagerService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();

            DotGraph.AbstractNode abstractNode = digraph.findNode(module);
            if (abstractNode == null) {
                final JahiaTemplatesPackage jahiaTemplatePackage = jahiaTemplateManagerService.getAnyDeployedTemplatePackage(module);
                final String options;
                if (jahiaTemplatePackage != null) {
                    if (jahiaTemplatePackage.getGroupId().equals(GROUP_ID_JAHIA_MODULES)) {
                        options = "style=filled,fillcolor=mediumseagreen,color=black,fontcolor=black,fontname=\"Verdana\",fontsize=9";
                    } else {
                        options = "style=filled,fillcolor=mediumslateblue,color=black,fontcolor=black,fontname=\"Verdana\",fontsize=9";
                    }
                    abstractNode = digraph.addNode(module).setLabel(module).setOptions(options);
                } else {
                    abstractNode = digraph.addNode(module).setLabel(module).setOptions(DotStyles.STUB_NODE_OPTIONS);
                }
            }

            for (String dependency : dependencies) {
                final JahiaTemplatesPackage jahiaTemplatePackage = jahiaTemplateManagerService.getAnyDeployedTemplatePackage(dependency);
                if (jahiaTemplatePackage != null) {
                    final String options;
                    if (jahiaTemplatePackage.getGroupId().equals(GROUP_ID_JAHIA_MODULES)) {
                        options = "style=filled,fillcolor=mediumseagreen,color=black,fontcolor=black,fontname=\"Verdana\",fontsize=9";
                    } else {
                        options = "style=filled,fillcolor=mediumslateblue,color=black,fontcolor=black,fontname=\"Verdana\",fontsize=9";
                    }
                    digraph.addNode(dependency).setLabel(dependency).setOptions(options);
                } else {
                    digraph.addNode(dependency).setLabel(dependency).setOptions(DotStyles.STUB_NODE_OPTIONS);
                }
                abstractNode.addAssociation(module, dependency).setOptions(style);
            }
        }
    }

    private Map<String, Set<String>> getCurrentDependencies() {
        final Map<String, Set<String>> poms = new TreeMap<>();
        AbstractDependenciesAnalysis.getActiveModules().forEach(module -> {
            final Set<String> dependencies = new TreeSet<>();
            module.getDependencies().forEach(dependency -> {
                dependencies.add(dependency.getBundle().getSymbolicName());
            });
            poms.put(module.getBundle().getSymbolicName(), dependencies);
        });
        return poms;
    }

    public void setCurrentDependenciesResults(Map<String, Set<String>> currentDependencies, List<DependenciesResults> results) {
        currentDependencies.entrySet().forEach(entry -> {
            final String module = entry.getKey();
            final Set<String> dependencies = entry.getValue();
            results.add(new DependenciesResults("current", "", module, dependencies));
        });
    }

    private static JCRNodeWrapper mkdirs(String path) throws RepositoryException {
        final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null);
        JCRNodeWrapper folderNode = session.getRootNode();
        for (String folder : path.split("\\/")) {
            if (!folder.isEmpty()) {
                if (folderNode.hasNode(folder)) {
                    folderNode = folderNode.getNode(folder);
                } else {
                    folderNode = folderNode.addNode(folder, "jnt:folder");
                }
            }
        }
        return folderNode;
    }
}
