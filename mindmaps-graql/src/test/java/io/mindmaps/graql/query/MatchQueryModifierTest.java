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

package io.mindmaps.graql.query;

import com.google.common.collect.Lists;
import io.mindmaps.MindmapsGraph;
import io.mindmaps.graph.internal.AbstractMindmapsGraph;
import io.mindmaps.concept.Concept;
import io.mindmaps.example.MovieGraphFactory;
import io.mindmaps.factory.MindmapsTestGraphFactory;
import io.mindmaps.graql.MatchQuery;
import io.mindmaps.graql.QueryBuilder;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.mindmaps.util.Schema.ConceptProperty.ITEM_IDENTIFIER;
import static io.mindmaps.graql.Graql.neq;
import static io.mindmaps.graql.Graql.or;
import static io.mindmaps.graql.Graql.var;
import static io.mindmaps.graql.Graql.withGraph;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MatchQueryModifierTest {

    private static MindmapsGraph mindmapsGraph;
    private QueryBuilder qb;

    @BeforeClass
    public static void setUpClass() {
        mindmapsGraph = MindmapsTestGraphFactory.newEmptyGraph();
        MovieGraphFactory.loadGraph(mindmapsGraph);
    }

    @Before
    public void setUp() {
        qb = withGraph(mindmapsGraph);
    }

    @Test
    public void testOffsetQuery() {
        MatchQuery query = qb.match(var("x").isa("movie")).orderBy("x", false).offset(4);

        assertResultsOrderedById(query, "x", false);
    }

    @Test
    public void testLimitQuery() {
        MatchQuery query = qb.match(var("x").isa("movie")).orderBy("x", true).offset(1).limit(3);

        assertResultsOrderedById(query, "x", true);
        assertEquals(3, query.stream().count());
    }

    @Test
    public void testOrPatternOrderByResource() {
        MatchQuery query = qb.match(
                var("x").isa("movie"),
                var().rel("x").rel("y"),
                or(
                        var("y").isa("person").id("Marlon-Brando"),
                        var("y").isa("genre").has("name", "crime")
                )
        ).orderBy("x", "tmdb-vote-count", false).select("x");

        assertOrderedResultsMatch(query, "x", "movie", "Godfather", "Godfather", "Apocalypse-Now", "Heat");
    }

    @Test
    public void testOrPatternOrderByUnselected() {
        MatchQuery query = qb.match(
                var("x").isa("movie"),
                var().rel("x").rel("y"),
                or(
                        var("y").isa("person"),
                        var("y").isa("genre").value(neq("crime"))
                )
        ).orderBy("y").offset(4).limit(8).select("x");

        QueryUtil.assertResultsMatch(
                query, "x", "movie", "Hocus-Pocus", "Spy", "The-Muppets", "Godfather", "Apocalypse-Now"
        );
    }

    @Test
    public void testDegreeOrderedQuery() {
        MatchQuery query = qb.match(var("the-movie").isa("movie")).orderBy("the-movie", false);

        assertResultsOrderedById(query, "the-movie", false);

        // Make sure all results are included
        QueryUtil.assertResultsMatch(query, "the-movie", "movie", QueryUtil.movies);
    }

    @Test
    public void testVoteCountOrderedQuery() {
        MatchQuery query = qb.match(var("z").isa("movie")).orderBy("z", "tmdb-vote-count", false);

        // Make sure movies are in the correct order
        assertOrderedResultsMatch(query, "z", "movie", "Godfather", "Hocus-Pocus", "Apocalypse-Now", "The-Muppets");

        // Movies without a tmdb-vote-count will still be at the end of the results
        QueryUtil.assertResultsMatch(query, "z", "movie", QueryUtil.movies);
    }

    @Test
    public void testOrPatternDistinct() {
        MatchQuery query = qb.match(
                var("x").isa("movie"),
                var().rel("x").rel("y"),
                or(
                        var("y").isa("genre").has("name", "crime"),
                        var("y").isa("person").id("Marlon-Brando")
                )
        ).select("x").orderBy("x", false).distinct();

        assertOrderedResultsMatch(query, "x", "movie", "Heat", "Godfather", "Apocalypse-Now");
    }

    @Test
    public void testNondistinctQuery() {
        MatchQuery query = qb.match(
                var("x").isa("person"),
                var("y").has("title", "The Muppets"),
                var().rel("x").rel("y")
        ).select("x");
        List<Map<String, Concept>> nondistinctResults = Lists.newArrayList(query);

        QueryUtil.assertResultsMatch(query, "x", "person", "Kermit-The-Frog", "Miss-Piggy");
        assertEquals(4, nondistinctResults.size());
    }

    @Test
    public void testDistinctQuery() {
        MatchQuery query = qb.match(
                var("x").isa("person"),
                var("y").has("title", "The Muppets"),
                var().rel("x").rel("y")
        ).distinct().select("x");
        List<Map<String, Concept>> distinctResults = Lists.newArrayList(query);

        QueryUtil.assertResultsMatch(query, "x", "person", "Kermit-The-Frog", "Miss-Piggy");
        assertEquals(2, distinctResults.size());
    }

    private void assertOrderedResultsMatch(MatchQuery query, String var, String expectedType, String... expectedIds) {
        Queue<String> expectedQueue = new LinkedList<>(Arrays.asList(expectedIds));

        query.forEach(results -> {
            assertEquals(1, results.size());
            Concept result = results.get(var);
            assertNotNull(result);

            String expectedId = expectedQueue.poll();
            if (expectedId != null) assertEquals(expectedId, result.getId());
            if (expectedType != null) assertEquals(expectedType, result.type().getId());
        });

        assertTrue("expected ids not found: " + expectedQueue, expectedQueue.isEmpty());
    }

    private void assertResultsOrderedById(MatchQuery query, String var, boolean asc) {
        GraphTraversalSource g = ((AbstractMindmapsGraph) mindmapsGraph).getTinkerTraversal();
        Stream<String> ids = query.stream().map(results -> results.get(var)).map(
                result -> (String) g.V().has("ITEM_IDENTIFIER", result.getId()).values(ITEM_IDENTIFIER.name()).next()
        );
        assertResultsOrdered(ids, asc);
    }

    private <T extends Comparable<T>> void assertResultsOrdered(Stream<T> results, boolean asc) {
        T previous = null;

        for (T result : results.collect(Collectors.toList())) {
            if (previous != null) {
                if (asc) {
                    assertTrue(result.compareTo(previous) >= 0);
                } else {
                    assertTrue(result.compareTo(previous) <= 0);
                }
            }
            previous = result;
        }
    }
}
