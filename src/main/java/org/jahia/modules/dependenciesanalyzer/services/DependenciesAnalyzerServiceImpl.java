package org.jahia.modules.dependenciesanalyzer.services;

import io.github.livingdocumentation.dotdiagram.DotGraph;
import io.github.livingdocumentation.dotdiagram.DotGraph.Digraph;
import io.github.livingdocumentation.dotdiagram.DotStyles;
import io.github.livingdocumentation.dotdiagram.GraphvizDotWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jahia.bin.Jahia;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalysis;
import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalyzerService;
import org.jahia.modules.dependenciesanalyzer.services.impl.AbstractDependenciesAnalysis;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;

@Component(name = "org.jahia.modules.dependenciesanalyzer.service", service = DependenciesAnalyzerService.class, property = {
    Constants.SERVICE_PID + "=org.jahia.modules.dependenciesanalyzer.service",
    Constants.SERVICE_DESCRIPTION + "=Dependencies analyzer service",
    Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME}, immediate = true)
public class DependenciesAnalyzerServiceImpl implements DependenciesAnalyzerService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DependenciesAnalyzerServiceImpl.class);
    private static final String OUTPUT_EXTENSION = "jpg";
    private static final String DOT_PATH = "/usr/bin/dot";
    private static final String OUTPUT_FOLDER = "/tmp/graphviz/";
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
        logAndAppend(String.format("Dependencies analyses (%d):", nbAnalyses), lines);

        final List<DependenciesResults> currentDependenciesResults = new ArrayList<>();
        final Map<String, Set<String>> currentDependencies = getCurrentDependencies();
        setCurrentDependenciesResults(currentDependencies, currentDependenciesResults);

        final List<DependenciesResults> missingDependenciesResults = new ArrayList<>();
        dependenciesAnalyses.stream().map(dependenciesAnalysis -> {
            logAndAppend(String.format("   %s", dependenciesAnalysis), lines);
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
            dependenciesResults.getDependencies().forEach(dependency -> {
                logAndAppend(String.format("%s;%s;%s", module, type, dependency), lines);
            });
        });
        return lines;
    }

    @Override
    public void writeGraph(boolean skipJahiaModule) {

        final List<DependenciesResults> currentDependenciesResults = new ArrayList<>();
        final Map<String, Set<String>> currentDependencies = getCurrentDependencies();
        setCurrentDependenciesResults(currentDependencies, currentDependenciesResults);

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
            try {
                final String fileName = analysisName;
                final String command = String.format("%s -T %s -o %s %s", DOT_PATH, OUTPUT_EXTENSION, OUTPUT_FOLDER + fileName + "." + OUTPUT_EXTENSION, OUTPUT_FOLDER + fileName + ".dot");
                final GraphvizDotWriter dotWriter = new GraphvizDotWriter(OUTPUT_FOLDER, DOT_PATH, OUTPUT_EXTENSION, command);

                dotWriter.write(fileName, graph.render());
                dotWriter.render(fileName);
            } catch (IOException | InterruptedException ex) {
                LOGGER.error("Impossible to create graph", ex);
            }
        });

    }

    private void logAndAppend(String line, List<String> lines) {
        LOGGER.info(line);
        lines.add(line);
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
            results.add(new DependenciesResults("current", module, dependencies));
        });
    }
}
