package io.jentz.winter.java;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.jentz.winter.Graph;
import kotlin.jvm.functions.Function0;

import static io.jentz.winter.java.JWinter.instance;
import static io.jentz.winter.java.JWinter.instanceOrNull;
import static io.jentz.winter.java.JWinter.instancesOfType;
import static io.jentz.winter.java.JWinter.provider;
import static io.jentz.winter.java.JWinter.providerOrNull;
import static io.jentz.winter.java.JWinter.providersOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testInstancesOfType() {
        Set<String> set = instancesOfType(graph, String.class);
        assertEquals(4, set.size());
        assertTrue(set.contains("prototype b"));
    }

    @Test
    void testProvidersOfType() {
        Set<Function0<String>> set = providersOfType(graph, String.class);
        assertEquals(4, set.size());
        assertTrue(set.stream().map(Function0::invoke).collect(Collectors.toList()).contains("prototype c"));
    }

}
