package org.jahia.modules.dependenciesanalyzer.jcrcommands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.modules.dependenciesanalyzer.services.Utils;

@Command(scope = "utils", name = "print-missing-dependencies", description = "Print missing dependencies")
@Service
public class PrintMissingDependenciesCommand implements Action {

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        final ShellTable table = new ShellTable();
        table.column(new Col("Module"));
        table.column(new Col("Dependency type"));
        table.column(new Col("Dependency"));
        table.column(new Col("Description"));
        Utils.getDependenciesAnalyzerService().printDependenciesAnalysesList().forEach((line) -> {
            table.addRow().addContent(line.split(";"));
        });

        table.print(System.out, true);
        return null;
    }
}
