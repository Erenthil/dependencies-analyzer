package org.jahia.modules.dependenciesanalyzer.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jahia.modules.dependenciesanalyzer.services.DependenciesResults;

public interface DependenciesAnalysis {

    String getName();

    long getId();

    void setId(long id);

    void setExpectedDependencies();

    void calculateMissingDependencies(Map<String, Set<String>> currentDependencies, List<DependenciesResults> results);

    Map<String, String> buildOrigins();

    Map<String, Set<String>> getExpectedDependencies();
}
