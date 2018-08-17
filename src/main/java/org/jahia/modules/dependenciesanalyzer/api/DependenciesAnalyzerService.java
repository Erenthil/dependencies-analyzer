package org.jahia.modules.dependenciesanalyzer.api;

import java.util.List;

public interface DependenciesAnalyzerService {

    List<String> printDependenciesAnalysesList();

    void writeGraph(boolean skipJahiaModule);
}
