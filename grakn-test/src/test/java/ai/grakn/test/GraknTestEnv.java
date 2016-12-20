package ai.grakn.test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.grakn.GraknGraph;
import ai.grakn.engine.util.ConfigProperties;
import ai.grakn.factory.GraphFactory;
import ai.grakn.factory.SystemKeyspace;
import org.slf4j.LoggerFactory;

import ai.grakn.Grakn;
import ai.grakn.GraknGraphFactory;
import ai.grakn.engine.GraknEngineServer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import static ai.grakn.graql.Graql.var;

/**
 * <p>
 * Contains utility methods and statically initialized environment variables to control
 * Grakn unit tests. 
 * </p>
 * 
 * @author borislav
 *
 */
public interface GraknTestEnv {
    static final String CONFIG = System.getProperty("grakn.test-profile");
    static AtomicBoolean CASSANDRA_RUNNING = new AtomicBoolean(false);
    static AtomicBoolean HTTP_RUNNING = new AtomicBoolean(false);

    static void hideLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.OFF);
    }

    static void startGraph() throws Exception {
        if (CASSANDRA_RUNNING.compareAndSet(false, true) && usingTitan()) {
            startEmbeddedCassandra();
            System.out.println("CASSANDRA RUNNING.");
        }

        if(HTTP_RUNNING.compareAndSet(false, true))
            GraknEngineServer.startHTTP();
    }

    static void stopGraph() throws Exception {
        if(HTTP_RUNNING.compareAndSet(true, false)) {
            // Drop all keyspaces
            GraknGraph systemGraph = GraphFactory.getInstance().getGraph(ConfigProperties.SYSTEM_GRAPH_NAME);
            systemGraph.graql().match(var("x").isa("keyspace-name"))
                    .execute()
                    .forEach(x -> x.values().forEach(y -> {
                        String name = y.asResource().getValue().toString();
                        GraknGraph graph = GraphFactory.getInstance().getGraph(name);
                        graph.clear();
                        System.out.println("Cleared " + name);
                    }));

            // Drop the system keyspaces too, and re-create it for the next run.
            systemGraph.clear();
            //String config = ConfigProperties.getInstance().getPath(ConfigProperties.GRAPH_CONFIG_PROPERTY);
            new SystemKeyspace(null, ConfigProperties.getInstance().getProperties()).loadSystemOntology();
            GraknEngineServer.stopHTTP();
        }
    }

    static GraknGraphFactory factoryWithNewKeyspace() {
        String keyspace;
        if (usingOrientDB()) {
            keyspace = "memory";
        } else {
            // Embedded Casandra has problems dropping keyspaces that start with a number
            keyspace = "a"+UUID.randomUUID().toString().replaceAll("-", "");
        }
        return Grakn.factory(Grakn.DEFAULT_URI, keyspace);
    }

    static void startEmbeddedCassandra() {
        try {
            // We have to use reflection here because the cassandra dependency is only included when testing the titan profile.
            Class cl = Class.forName("org.cassandraunit.utils.EmbeddedCassandraServerHelper");
            hideLogs();

            //noinspection unchecked
            cl.getMethod("startEmbeddedCassandra", String.class).invoke(null, "cassandra-embedded.yaml");
            hideLogs();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean usingTinker() {
        return "tinker".equals(CONFIG);
    }

    static boolean usingTitan() {
        return "titan".equals(CONFIG);
    }

    static boolean usingOrientDB() {
        return "orientdb".equals(CONFIG);
    }
}
