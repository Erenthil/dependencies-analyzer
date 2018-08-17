package org.jahia.modules.dependenciesanalyzer.jcrcommands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.jahia.modules.dependenciesanalyzer.services.Utils;

@Command(scope = "utils", name = "graph-missing-dependencies", description = "Print missing dependencies")
@Service
public class GraphMissingDependenciesCommand implements Action {

    @Reference
    Session session;

    @Option(name = "-s", aliases = "--skip", description = "Ignore modules created by Jahia")
    private boolean skip;

    @Override
    public Object execute() throws Exception {
        Utils.getDependenciesAnalyzerService().writeGraph(skip);
        return null;
    }
}
