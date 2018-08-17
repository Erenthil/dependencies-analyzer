package org.jahia.modules.dependenciesanalyzer.jcrcommands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.jahia.modules.dependenciesanalyzer.services.Utils;

@Command(scope = "utils", name = "print-missing-dependencies", description = "Print missing dependencies")
@Service
public class PrintMissingDependenciesCommand implements Action {

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        Utils.getDependenciesAnalyzerService().printDependenciesAnalysesList();
        return null;
    }
}
