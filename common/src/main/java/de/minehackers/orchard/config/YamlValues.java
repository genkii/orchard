package de.minehackers.orchard.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import de.minehackers.orchard.pack.PackLoadException;

/// Helpers for pulling typed values out of a decoded YAML document.
public final class YamlValues {

    private YamlValues() {}

    public static Map<?, ?> asMap(Object value, String where) {
        if (value instanceof Map<?, ?> map) return map;
        throw new PackLoadException(where + ": expected a mapping, got " + typeName(value));
    }

    public static List<?> asList(Object value, String where) {
        if (value instanceof List<?> list) return list;
        throw new PackLoadException(where + ": expected a list, got " + typeName(value));
    }

    public static String asString(Object value, String where) {
        if (value instanceof String s && !s.isBlank()) return s.trim();
        if (value instanceof Number || value instanceof Boolean) {
            return canonicalScalar(value);
        }
        throw new PackLoadException(where + ": expected text, got " + typeName(value));
    }

    public static int asInt(Object value, String where) {
        if (value instanceof Number n) {
            double d = n.doubleValue();
            if (d != Math.floor(d)) {
                throw new PackLoadException(where + ": expected an integer, got " + n);
            }
            return n.intValue();
        }
        throw new PackLoadException(where + ": expected an integer, got " + typeName(value));
    }

    public static double asNumber(Object value, String where) {
        if (value instanceof Number n) return n.doubleValue();
        throw new PackLoadException(where + ": expected a number, got " + typeName(value));
    }

    public static boolean asBool(Object value, String where) {
        if (value instanceof Boolean b) return b;
        throw new PackLoadException(where + ": expected true or false, got " + typeName(value));
    }

    /// Renders scalars roughly the way the author wrote them, avoiding 1.0E-1 style noise.
    public static String describe(Object value) {
        if (value == null) return "nothing";
        return "'" + (value instanceof String s ? s : canonicalScalar(value)) + "'";
    }

    private static String typeName(Object value) {
        if (value == null) return "nothing";
        if (value instanceof String) return "text";
        if (value instanceof Integer || value instanceof Long) return "an integer";
        if (value instanceof Number) return "a decimal number";
        if (value instanceof Boolean) return "a boolean";
        if (value instanceof Map) return "a mapping";
        if (value instanceof List) return "a list";
        return value.getClass().getSimpleName();
    }

    private static String canonicalScalar(Object value) {
        if (value instanceof Double d) return stripTrailingZero(d);
        if (value instanceof Float f) return stripTrailingZero(f.doubleValue());
        return String.valueOf(value);
    }

    private static String stripTrailingZero(double d) {
        if (d == Math.floor(d) && Math.abs(d) < 1e15) {
            long l = (long) d;
            if (l == d) return String.valueOf(l) + ".0";
        }
        return String.valueOf(d);
    }

    public static List<String> stringList(Object value, String where) {
        List<?> raw = asList(value, where);
        List<String> out = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            out.add(asString(raw.get(i), where + "[" + i + "]"));
        }
        return out;
    }
}
