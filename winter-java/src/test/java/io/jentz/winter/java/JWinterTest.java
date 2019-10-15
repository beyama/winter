package io.jentz.winter.java;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.jentz.winter.Graph;
import kotlin.jvm.functions.Function0;

import static io.jentz.winter.java.JWinter.factory;
import static io.jentz.winter.java.JWinter.factoryOrNull;
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
    void testInstanceWithArgument() {
        assertEquals(instance(graph, Integer.class, String.class, 42), "42");
    }

    @Test
    void testInstanceOrNull() {
        assertNull(instanceOrNull(graph, List.class));
        assertEquals(instanceOrNull(graph, String.class), "prototype");
        assertEquals(instanceOrNull(graph, String.class, "a"), "prototype a");
    }

    @Test
    void testInstanceOrNullWithArgument() {
        assertNull(instanceOrNull(graph, String.class, String.class, "arg"));
        assertEquals(instanceOrNull(graph, Integer.class, String.class, 42), "42");
    }

    @Test
    void testProvider() {
        assertEquals(provider(graph, String.class).invoke(), "prototype");
        assertEquals(provider(graph, String.class, "a").invoke(), "prototype a");
    }

    @Test
    void testProviderWithArgument() {
        assertEquals(provider(graph, Integer.class, String.class, 42).invoke(), "42");
    }

    @Test
    void testProviderOrNull() {
        assertNull(providerOrNull(graph, List.class));
        assertEquals(providerOrNull(graph, String.class).invoke(), "prototype");
        assertEquals(providerOrNull(graph, String.class, "a").invoke(), "prototype a");
    }

    @Test
    void testProviderOrNullWithArgument() {
        assertNull(providerOrNull(graph, String.class, String.class, "arg"));
        assertEquals(providerOrNull(graph, Integer.class, String.class, 42).invoke(), "42");
    }

    @Test
    void testFactory() {
        assertEquals(factory(graph, Integer.class, String.class).invoke(42), "42");
    }

    @Test
    void testFactoryOrNull() {
        assertNull(factoryOrNull(graph, String.class, String.class));
        assertEquals(factoryOrNull(graph, Integer.class, String.class).invoke(42), "42");
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
