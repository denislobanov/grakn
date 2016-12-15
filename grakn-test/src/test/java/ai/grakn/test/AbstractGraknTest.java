/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.test;

import static ai.grakn.test.GraknTestEnv.startGraph;
import static ai.grakn.test.GraknTestEnv.stopGraph;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Abstract test class that automatically starts the relevant graph database and provides a method to get a graph factory
 */
public abstract class AbstractGraknTest {
	
    @BeforeClass
    public static void initializeGraknTests() throws Exception {
    	startGraph();
    }

    @AfterClass
    public static void cleanupGraknTests() throws Exception {
    	stopGraph();
    }
}
