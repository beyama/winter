package test;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class WithInjectedGenericFields {
    @Inject @NotNull Map<String, Integer> field0;
    @Inject List<Integer> field1;
}