package org.jahia.modules.dependenciesanalyzer.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalysis;
import org.jahia.modules.dependenciesanalyzer.services.DependenciesResults;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.templates.JahiaTemplateManagerService;

public abstract class AbstractDependenciesAnalysis implements DependenciesAnalysis {

    protected static final List<String> MODULES_TO_IGNORE = Arrays.asList("system-jahia", "system-system");
    private final Map<String, Set<String>> expectedDependencies = new TreeMap<>();
    private final String type;
    private String description;
    private long id = -1L;

    public AbstractDependenciesAnalysis(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("%s (id: %s)", getName(), getId());
    }

    @Override
    public void setExpectedDependencies() {
        final Map<String, String> origins = buildOrigins();
        getExpectedDependencies().entrySet().forEach((entry) -> {
            final String module = entry.getKey();
            final Set<String> dependencies = entry.getValue();
            final Set<String> realDependencies = new HashSet<>();
            expectedDependencies.put(module, realDependencies);
            dependencies.forEach((dependency) -> {
                if (origins.containsKey(dependency)) {
                    final String dependencyModule = origins.get(dependency);
                    if (!MODULES_TO_IGNORE.contains(dependencyModule) && !dependencyModule.equals(module)) {
                        realDependencies.add(dependencyModule);
                    }
                } else {
                    realDependencies.add(dependency);
                }
            });
        });
    }

    @Override
    public void calculateMissingDependencies(Map<String, Set<String>> currentDependencies, List<DependenciesResults> results) {
        final JahiaTemplateManagerService jahiaTemplateManagerService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();
        expectedDependencies.entrySet().forEach((entry) -> {
            final String module = entry.getKey();
            final Set<String> dependencies = entry.getValue();
            if (currentDependencies.containsKey(module)) {
                dependencies.removeAll(currentDependencies.get(module));
            }
            results.add(new DependenciesResults(type, module, dependencies));
        });
    }

    public static List<JahiaTemplatesPackage> getActiveModules() {
        final List<JahiaTemplatesPackage> activeModules = new ArrayList<>();
        ServicesRegistry.getInstance().getJahiaTemplateManagerService().getAvailableTemplatePackages().stream()
                .filter((module) -> (module.isActiveVersion())).forEachOrdered((module) -> {
            activeModules.add(module);

        });
        return activeModules;
    }
}
