package ai.grakn.test;

import static java.lang.Thread.sleep;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import ai.grakn.Grakn;
import ai.grakn.GraknGraphFactory;
import ai.grakn.engine.GraknEngineServer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

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

    static void hideLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.OFF);
    }

    static void startGraph() throws Exception {
        if (CASSANDRA_RUNNING.compareAndSet(false, true) && usingTitan()) {
            startEmbeddedCassandra();
            System.out.println("CASSANDRA RUNNING.");
        }

        GraknEngineServer.startHTTP();
        sleep(5000);
    }

    static void stopGraph() throws Exception {
        GraknEngineServer.stopHTTP();
        sleep(5000);

        if (usingTitan()) {
            clearEmbeddedCassandra();
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

    static void clearEmbeddedCassandra() {
        try {
            Class cl = Class.forName("org.cassandraunit.utils.EmbeddedCassandraServerHelper");
            cl.getMethod("cleanEmbeddedCassandra").invoke(null);
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
