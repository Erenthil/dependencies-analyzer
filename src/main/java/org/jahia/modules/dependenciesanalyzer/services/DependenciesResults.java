package org.jahia.modules.dependenciesanalyzer.services;

import java.util.Set;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalyzerService;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.templates.JahiaTemplateManagerService;

public final class DependenciesResults {

    private final String type;
    private final String module;
    private final boolean jahiaModule;
    private final Set<String> dependencies;

    public DependenciesResults(String type, String module, Set<String> dependencies) {
        this.type = type;
        this.module = module;
        this.dependencies = dependencies;
        final JahiaTemplateManagerService jahiaTemplateManagerService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();
        final JahiaTemplatesPackage jahiaTemplatePackage = jahiaTemplateManagerService.getAnyDeployedTemplatePackage(module);
        this.jahiaModule = jahiaTemplatePackage.getGroupId().equals(DependenciesAnalyzerService.GROUP_ID_JAHIA_MODULES);

    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public String getModule() {
        return module;
    }

    public boolean isJahiaModule() {
        return jahiaModule;
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return module + ":" + type;
    }
}
