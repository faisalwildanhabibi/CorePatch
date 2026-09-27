package org.lsposed.corepatch.zygisk;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utils — Reflection and method-matching utilities.
 *
 * Adapted from PMPatch by vova7878.
 */
public class Utils {

    /** Get all declared methods + constructors from a class. */
    public static Executable[] getAllExecutables(Class<?> clazz) {
        List<Executable> list = new ArrayList<>();
        list.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        list.addAll(Arrays.asList(clazz.getDeclaredConstructors()));
        return list.toArray(new Executable[0]);
    }

    private static String clname(Class<?> clazz) {
        Class<?> component = clazz.getComponentType();
        if (component != null) return clname(component) + "[]";
        return clazz.getName();
    }

    private static String name(Executable e) {
        if (e instanceof Method m) return m.getName();
        return Modifier.isStatic(e.getModifiers()) ? "<clinit>" : "<init>";
    }

    private static String ret(Executable e) {
        if (e instanceof Method m) return clname(m.getReturnType());
        return clname(void.class);
    }

    private static String[] args(Executable e) {
        return Stream.of(e.getParameterTypes())
            .map(Utils::clname).toArray(String[]::new);
    }

    private static String printExecutable(Executable e) {
        return String.format("%s(%s)%s",
            name(e), String.join(", ", args(e)), ret(e));
    }

    /** Build a predicate that matches executables by a regex pattern on their signature string. */
    public static Predicate<Executable> filter(String pattern) {
        Objects.requireNonNull(pattern);
        Pattern compiled = Pattern.compile(pattern);
        return e -> compiled.matcher(printExecutable(e)).matches();
    }
}
