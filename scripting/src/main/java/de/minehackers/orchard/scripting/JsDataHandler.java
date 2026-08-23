package de.minehackers.orchard.scripting;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import de.minehackers.orchard.config.DataFileParser;
import de.minehackers.orchard.config.RawDefinition;
import de.minehackers.orchard.pack.PackDataHandler;
import de.minehackers.orchard.pack.PackLoadException;

/// Pack data handler for .js definition scripts. Each script runs once at
/// pack load (never during world generation) and registers definitions by
/// calling define(...) with an object or a list - same fields and selector
/// syntax as YAML data files, validated by the same parser code. An orchard
/// helper namespace adds sugar (biome, tag, block, log). Scripts are fully
/// sandboxed: safe standard JS objects only, no way to touch Java classes,
/// and a hard 5-second timeout per script.
public final class JsDataHandler implements PackDataHandler {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final long TIMEOUT_NANOS = 5_000_000_000L; // 5s per script

    /// Timeout deadline of whichever script is currently running on this thread.
    private static final ThreadLocal<Long> DEADLINE = new ThreadLocal<>();

    /// The sandbox factory. Everything that keeps scripts safe comes together
    /// here: interpreter mode only, safe standard objects, no access to Java
    /// classes, and the per-script timeout wiring.
    private static final ContextFactory FACTORY = new ScriptContextFactory();

    private static final class ScriptContextFactory extends ContextFactory {
        @Override
        protected Context makeContext() {
            TimeoutContext cx = new TimeoutContext();
            cx.setLanguageVersion(Context.VERSION_ES6);
            // Interpreter mode on purpose: these scripts only ever run once
            // at pack load, so JIT-generated classes would buy us nothing and
            // just add classloader headaches. The optimizer API being
            // deprecated is fine by us - Rhino wants to drop it anyway.
            //noinspection deprecation
            cx.setOptimizationLevel(0);
            cx.setInstructionObserverThreshold(10_000);
            // Deny every script-initiated Java class access.
            cx.setClassShutter(className -> false);
            return cx;
        }
    }

    /// Checks the clock often enough to enforce the 5s timeout. Subclassing
    /// Context inside a factory is the supported way to use instruction
    /// observation - the constructor deprecation only warns about creating
    /// contexts directly outside a factory.
    @SuppressWarnings("deprecation")
    private static final class TimeoutContext extends Context {
        @Override
        protected void observeInstructionCount(int instructionCount) {
            Long deadline = DEADLINE.get();
            if (deadline != null && System.nanoTime() > deadline) {
                throw new RuntimeException("script timed out after "
                        + (TIMEOUT_NANOS / 1_000_000) + " ms");
            }
        }
    }

    public JsDataHandler() {}

    @Override
    public Set<String> supportedExtensions() {
        return Set.of("js");
    }

    @Override
    public List<RawDefinition> parse(Path file) throws PackLoadException {
        String name = file.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".js")) {
            throw new PackLoadException(name + ": not a .js script");
        }

        String source = readSource(file, name);

        List<Object> collected = new ArrayList<>();
        DEADLINE.set(System.nanoTime() + TIMEOUT_NANOS);
        try {
            FACTORY.call(context -> {
                evaluate(context, name, source, collected);
                return null;
            });
        } catch (PackLoadException e) {
            throw e;
        } catch (RhinoException e) {
            throw new PackLoadException(describe(name, e), e);
        } catch (Exception e) {
            throw new PackLoadException(name + ": " + e.getMessage(), e);
        } finally {
            DEADLINE.remove();
        }

        if (collected.isEmpty()) {
            throw new PackLoadException(
                    name + ": no definitions created - call define({...}) at least once");
        }

        List<RawDefinition> out = new ArrayList<>(collected.size());
        for (int i = 0; i < collected.size(); i++) {
            Object entry = collected.get(i);
            String where = name + " define #" + (i + 1);
            if (entry instanceof List<?> list) {
                // define([ {...}, {...} ]) - same shape as a YAML list file.
                for (int j = 0; j < list.size(); j++) {
                    Map<?, ?> element = asMap(list.get(j), where + " entry #" + (j + 1));
                    out.add(DataFileParser.fromMap(element, where + " entry #" + (j + 1)));
                }
                continue;
            }
            Map<?, ?> map = asMap(entry, where);
            out.add(DataFileParser.fromMap(map, where));
        }
        return out;
    }

    // --- evaluation ---

    private static void evaluate(Context cx, String name, String source, List<Object> out) {
        cx.setLanguageVersion(Context.VERSION_ES6);
        // Interpreter mode again, same reasoning as in the factory: scripts
        // only run once at load time, so the JIT buys nothing here.
        cx.setOptimizationLevel(0);

        ScriptableObject scope = cx.initSafeStandardObjects(new NativeObject(), false);

        ScriptableObject.putProperty(scope, "define", new BaseFunction() {
            @Override
            public String getFunctionName() {
                return "define";
            }

            @Override
            public int getArity() {
                return 1;
            }

            @Override
            public Object call(Context c, Scriptable s, Scriptable thisObj, Object[] args) {
                if (args.length == 0 || args[0] == null || Undefined.instance.equals(args[0])) {
                    throw new RuntimeException("define(...) needs a definition object or a list");
                }
                out.add(toJava(args[0], "define(...)"));
                return Undefined.instance;
            }
        });

        ScriptableObject orchard = (ScriptableObject) cx.newObject(scope);
        putHelper(orchard, "biome", args -> {
            String raw = stringArg(args, "orchard.biome(name)");
            return normalize(raw, "#", null);
        });
        putHelper(orchard, "tag", args -> {
            String raw = stringArg(args, "orchard.tag(name)");
            return raw.startsWith("#") ? normalize(raw.substring(1), "#", null) : normalize(raw, "#", null);
        });
        putHelper(orchard, "block", args -> normalize(stringArg(args, "orchard.block(id)"), "", "minecraft"));
        putHelper(orchard, "log", args -> {
            de.minehackers.orchard.Constants.LOG.info("[Orchard/JS] {}",
                    stringArg(args, "orchard.log(message)"));
            return Undefined.instance;
        });
        ScriptableObject.putProperty(scope, "orchard", orchard);

        cx.evaluateString(scope, source, name, 1, null);
    }

    private interface HelperFn {
        Object apply(Object[] args);
    }

    private static void putHelper(ScriptableObject target, String name, HelperFn fn) {
        ScriptableObject.putProperty(target, name, new BaseFunction() {
            @Override
            public String getFunctionName() {
                return name;
            }

            @Override
            public Object call(Context c, Scriptable s, Scriptable thisObj, Object[] args) {
                Object result = fn.apply(args);
                return result == null ? Undefined.instance : result;
            }
        });
    }

    private static String stringArg(Object[] args, String where) {
        if (args.length < 1 || !(args[0] instanceof CharSequence cs)) {
            throw new RuntimeException(where + " needs a text argument");
        }
        return cs.toString();
    }

    private static String normalize(String raw, String prefix, String forcedNamespace) {
        String trimmed = raw.trim();
        if (trimmed.contains(":")) {
            return prefix + trimmed;
        }
        return prefix + "minecraft:" + trimmed;
    }

    // --- conversion JS -> plain Java ---

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value, String where) {
        if (!(value instanceof Map)) {
            throw new PackLoadException(where + ": expected an object with fields like 'nbt'");
        }
        return (Map<String, Object>) value;
    }

    private static Object toJava(Object value, String where) {
        switch (value) {
            case null:
                return null;
            case Wrapper wrapper:
                return toJava(wrapper.unwrap(), where);
            case String s:
                return s;
            case CharSequence cs:
                return cs.toString();
            case Boolean b:
                return b;
            case Number n:
                return number(n);
            case NativeObject obj: {
                Map<String, Object> map = new LinkedHashMap<>(obj.size());
                for (Object keyObj : obj.getIds()) {
                    Object child;
                    if (keyObj instanceof Number n) {
                        child = obj.get(n.intValue(), obj);
                    } else {
                        child = obj.get(String.valueOf(keyObj), obj);
                    }
                    if (isMissing(child)) continue;
                    map.put(String.valueOf(keyObj), toJava(child, where));
                }
                return map;
            }
            case NativeArray array: {
                List<Object> list = new ArrayList<>((int) array.getLength());
                for (int i = 0; i < array.getLength(); i++) {
                    Object element = array.get(i, array);
                    if (isMissing(element)) continue;
                    list.add(toJava(element, where));
                }
                return list;
            }
            case BaseFunction ignored:
                throw new PackLoadException(where
                        + ": functions are not supported inside definition values (use plain data)");
            default:
                return String.valueOf(value);
        }
    }

    /// True for sparse array holes and deleted properties.
    private static boolean isMissing(Object value) {
        return value == Scriptable.NOT_FOUND || value instanceof org.mozilla.javascript.UniqueTag;
    }

    private static Object number(Number n) {
        double d = n.doubleValue();
        if (d == Math.rint(d) && Math.abs(d) <= Integer.MAX_VALUE) {
            return (int) d;
        }
        return d;
    }

    // --- helpers ---

    private static String readSource(Path file, String name) throws PackLoadException {
        try {
            long size = Files.size(file);
            if (size > MAX_FILE_SIZE) {
                throw new PackLoadException(name + ": file too large (" + size + " bytes)");
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder((int) size);
                char[] buffer = new char[8192];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                return sb.toString();
            }
        } catch (PackLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new PackLoadException(name + ": cannot read file: " + e.getMessage(), e);
        }
    }

    private static String describe(String name, RhinoException e) {
        int line = e.lineNumber();
        StringBuilder sb = new StringBuilder(name);
        if (line > 0) sb.append(':').append(line);
        String message = e.getMessage();
        if (message != null && message.startsWith(name)) {
            // Rhino sometimes prefixes the message with "<source>:<line>" itself.
            return message;
        }
        return sb.append(": ").append(message).toString();
    }
}
