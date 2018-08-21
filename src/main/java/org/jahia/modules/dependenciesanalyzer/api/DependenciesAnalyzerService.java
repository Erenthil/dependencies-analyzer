package org.jahia.modules.dependenciesanalyzer.api;

import java.util.List;

public interface DependenciesAnalyzerService {

    List<String> printDependenciesAnalysesList(boolean skipJahiaModule);

    void writeGraph(boolean skipJahiaModule);
}
