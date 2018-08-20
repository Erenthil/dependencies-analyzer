package org.jahia.modules.dependenciesanalyzer.services.checks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import org.jahia.modules.dependenciesanalyzer.api.DependenciesAnalysis;
import org.jahia.modules.dependenciesanalyzer.services.impl.AbstractDependenciesAnalysis;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.osgi.service.component.annotations.Component;

@Component(service = DependenciesAnalysis.class, immediate = true)
public class DefinitionsCheck extends AbstractDependenciesAnalysis {

    public DefinitionsCheck() {
        super("definitions", "Check that the Jahia dependencies match the mixins and node types used in the definitions"); 
    }

    @Override
    public Map<String, String> buildOrigins() {
        final Map<String, String> origins = new HashMap<>();
        final NodeTypeRegistry nodeTypeRegistry = NodeTypeRegistry.getInstance();
        final List<String> systemIds = nodeTypeRegistry.getSystemIds();
        systemIds.forEach((systemId) -> {
            final NodeTypeIterator nodeTypes = nodeTypeRegistry.getNodeTypes(systemId);
            while (nodeTypes.hasNext()) {
                final NodeType next = nodeTypes.nextNodeType();
                origins.put(next.getName(), systemId);
            }
        });
        return origins;
    }

    @Override
    public Map<String, Set<String>> getExpectedDependencies() {
        final Map<String, Set<String>> definitions = new TreeMap<>();
        final NodeTypeRegistry nodeTypeRegistry = NodeTypeRegistry.getInstance();
        getActiveModules().forEach((module) -> {
            final String moduleName = module.getId();
            if (!MODULES_TO_IGNORE.contains(moduleName)) {
                final NodeTypeIterator nodeTypes = nodeTypeRegistry.getNodeTypes(moduleName);
                final Set<String> dependencies = new TreeSet<>();
                definitions.put(moduleName, dependencies);

                while (nodeTypes.hasNext()) {
                    final NodeType next = nodeTypes.nextNodeType();
                    dependencies.addAll(Arrays.asList(next.getDeclaredSupertypeNames()));
                }
            }
        });

        return definitions;
    }
}
