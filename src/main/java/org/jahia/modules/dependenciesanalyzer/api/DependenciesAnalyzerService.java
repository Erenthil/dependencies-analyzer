package org.jahia.modules.dependenciesanalyzer.api;

import java.util.List;

public interface DependenciesAnalyzerService {

    public static final String GROUP_ID_JAHIA_MODULES = "org.jahia.modules";

    List<String> printDependenciesAnalysesList();

    void writeGraph(boolean skipJahiaModule);
}
