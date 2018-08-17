package org.jahia.modules.dependenciesanalyzer.services;

import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalyzerService;
import org.jahia.osgi.BundleUtils;

public class Utils {

    public static DependenciesAnalyzerService getDependenciesAnalyzerService() {
        return BundleUtils.getOsgiService(DependenciesAnalyzerService.class, null);
    }
}
