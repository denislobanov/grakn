/*
 * MindmapsDB - A Distributed Semantic Database
 * Copyright (C) 2016  Mindmaps Research Ltd
 *
 * MindmapsDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MindmapsDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package io.mindmaps.graql.internal.analytics;

import com.google.common.collect.Sets;
import io.mindmaps.util.Schema;
import io.mindmaps.util.ErrorMessage;
import io.mindmaps.concept.Type;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.*;
import org.apache.tinkerpop.gremlin.process.computer.util.ConfigurationTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.mindmaps.graql.internal.analytics.Analytics.*;

/**
 *
 */

public class DegreeVertexProgram implements VertexProgram<Long> {

    private final MessageScope.Local<Long> countMessageScopeIn = MessageScope.Local.of(__::inE);
    private final MessageScope.Local<Long> countMessageScopeOut = MessageScope.Local.of(__::outE);

    public static final String DEGREE = "analytics.degreeVertexProgram.degree";

    private static final String TRAVERSAL_SUPPLIER = "analytics.degreeVertexProgram.traversalSupplier";

    private ConfigurationTraversal<Vertex, Edge> configurationTraversal;

    private static final Set<String> COMPUTE_KEYS = Collections.singleton(DEGREE);

    private HashSet<String> baseTypes = Sets.newHashSet(
            Schema.BaseType.ENTITY.name(),
            Schema.BaseType.RESOURCE.name());

    private Set<String> selectedTypes = null;

    public DegreeVertexProgram() {
    }

    public DegreeVertexProgram(Set<String> types) {
        selectedTypes = types;
    }

    @Override
    public void loadState(final Graph graph, final Configuration configuration) {
        this.selectedTypes = new HashSet<>();
        configuration.getKeys(TYPE).forEachRemaining(key -> selectedTypes.add(configuration.getString(key)));
    }

    @Override
    public void storeState(final Configuration configuration) {
        configuration.setProperty(VERTEX_PROGRAM, DegreeVertexProgram.class.getName());
        Iterator iterator = selectedTypes.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            configuration.addProperty(TYPE + "." + count, iterator.next());
            count++;
        }
    }

    @Override
    public GraphComputer.ResultGraph getPreferredResultGraph() {
        return GraphComputer.ResultGraph.NEW;
    }

    @Override
    public GraphComputer.Persist getPreferredPersist() {
        return GraphComputer.Persist.VERTEX_PROPERTIES;
    }

    @Override
    public Set<String> getElementComputeKeys() {
        return COMPUTE_KEYS;
    }

    @Override
    public Set<MessageScope> getMessageScopes(final Memory memory) {
        final Set<MessageScope> set = new HashSet<>();
        set.add(this.countMessageScopeOut);
        set.add(this.countMessageScopeIn);
        return set;
    }

    @Override
    public DegreeVertexProgram clone() {
        try {
            final DegreeVertexProgram clone = (DegreeVertexProgram) super.clone();
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(ErrorMessage.CLONE_FAILED.getMessage(this.getClass().toString(),e.getMessage()),e);
        }
    }

    @Override
    public void setup(final Memory memory) {

    }

    @Override
    public void execute(final Vertex vertex, Messenger<Long> messenger, final Memory memory) {
        switch (memory.getIteration()) {
            case 0:
                if (selectedTypes.contains(getVertexType(vertex)) && !isAnalyticsElement(vertex)) {
                    if (baseTypes.contains(vertex.label())) {
                        messenger.sendMessage(this.countMessageScopeIn, 1L);
                    } else if (vertex.label().equals(Schema.BaseType.RELATION.name())) {
                        messenger.sendMessage(this.countMessageScopeIn, 1L);
                        messenger.sendMessage(this.countMessageScopeOut, -1L);
                    }
                }
                break;
            case 1:
                if (vertex.label().equals(Schema.BaseType.CASTING.name())) {
                    boolean hasRolePlayer = false;
                    long assertionCount = 0;
                    Iterator<Long> iterator = messenger.receiveMessages();
                    while (iterator.hasNext()) {
                        long message = iterator.next();
                        if (message < 0) assertionCount++;
                        else hasRolePlayer = true;
                    }
                    if (hasRolePlayer) {
                        messenger.sendMessage(this.countMessageScopeIn, 1L);
                        messenger.sendMessage(this.countMessageScopeOut, assertionCount);
                    }
                }
                break;
            case 2:
                if (!isAnalyticsElement(vertex) && selectedTypes.contains(getVertexType(vertex))) {
                    if (baseTypes.contains(vertex.label()) ||
                            vertex.label().equals(Schema.BaseType.RELATION.name())) {
                        long edgeCount = IteratorUtils.reduce(messenger.receiveMessages(), 0L, (a, b) -> a + b);
                        vertex.property(DEGREE, edgeCount);
                    }
                }
                break;
        }
    }

    @Override
    public boolean terminate(final Memory memory) {
        return memory.getIteration() == 2;
    }

    @Override
    public String toString() {
        return StringFactory.vertexProgramString(this);
    }

}
