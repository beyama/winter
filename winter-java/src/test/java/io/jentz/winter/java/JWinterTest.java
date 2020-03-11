package io.jentz.winter.java;

import org.junit.jupiter.api.Test;

import java.util.List;

import io.jentz.winter.Graph;

import static io.jentz.winter.java.JWinter.instance;
import static io.jentz.winter.java.JWinter.instanceOrNull;
import static io.jentz.winter.java.JWinter.provider;
import static io.jentz.winter.java.JWinter.providerOrNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("ConstantConditions")
class JWinterTest {

    private Graph graph = Di.getTestComponent().createGraph();

    @Test
    void testInstance() {
        assertEquals(instance(graph, String.class), "prototype");
        assertEquals(instance(graph, String.class, "a"), "prototype a");
    }

    @Test
    void testInstanceOrNull() {
        assertNull(instanceOrNull(graph, List.class));
        assertEquals(instanceOrNull(graph, String.class), "prototype");
        assertEquals(instanceOrNull(graph, String.class, "a"), "prototype a");
    }

    @Test
    void testProvider() {
        assertEquals(provider(graph, String.class).invoke(), "prototype");
        assertEquals(provider(graph, String.class, "a").invoke(), "prototype a");
    }

    @Test
    void testProviderOrNull() {
        assertNull(providerOrNull(graph, List.class));
        assertEquals(providerOrNull(graph, String.class).invoke(), "prototype");
        assertEquals(providerOrNull(graph, String.class, "a").invoke(), "prototype a");
    }

}
