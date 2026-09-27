package org.lsposed.corepatch.zygisk;

import static com.v7878.hooks.pmpatch.Main.TAG;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;

import android.util.Log;

import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.HookTransformer;

/**
 * HTF (Hook Transform Functions) — Pre-built hook implementations.
 *
 * Adapted from PMPatch by vova7878, extended for ZygiskCore.
 *
 * Usage:
 *   HTF.NOP   — suppress method entirely (void return)
 *   HTF.FALSE — return false (or default/null)
 *   HTF.TRUE  — return true
 *   HTF.constant(value) — return any constant value
 *   HTF.constant(value, run, exclude) — conditional return based on call stack
 */
public class HTF {
    private static final String TAG = Main.TAG;

    public static class StackException extends Exception {
        public StackException() { super("", null, true, false); }
    }

    private static void printStackTrace(EmulatedStackFrame frame) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, frame.toString(), new StackException());
        }
    }

    /** Suppress method call (returns default/null/false/void). */
    public static final HookTransformer NOP = (original, frame) -> {
        printStackTrace(frame);
    };

    /** Same as NOP — for boolean methods returning false by default. */
    public static final HookTransformer FALSE = NOP;

    /** Return true (or set boolean result to true). */
    public static final HookTransformer TRUE = (original, frame) -> {
        printStackTrace(frame);
        Class<?> ret = frame.type().returnType();
        if (ret == boolean.class) {
            frame.accessor().setBoolean(RETURN_VALUE_IDX, true);
        } else if (ret == void.class) {
            // void — NOP is sufficient
        } else {
            Log.e(TAG, "HTF.TRUE: unexpected return type: " + ret, new StackException());
            Transformers.invokeExact(original, frame);
        }
    };

    /** Return a constant value for any return type. */
    public static HookTransformer constant(Object value) {
        return (original, frame) -> {
            printStackTrace(frame);
            if (frame.type().returnType() != void.class) {
                frame.accessor().setValue(RETURN_VALUE_IDX, value);
            }
        };
    }

    /**
     * Conditionally return a constant based on call stack inspection.
     *
     * @param value   Value to return when condition is met.
     * @param run     Method names that MUST appear in the call stack (null = always run).
     * @param exclude Method names that MUST NOT appear in the call stack.
     *
     * Used for checkCapability: only intercept when called from installPackagesLI/
     * preparePackageLI, NOT from reconcilePackages.
     */
    public static HookTransformer constant(Object value, String[] run, String[] exclude) {
        return (original, frame) -> {
            printStackTrace(frame);

            boolean runFlag = (run == null);
            boolean excludeFlag = false;

            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                String name = element.getMethodName();
                if (!runFlag && contains(run, name)) runFlag = true;
                if (exclude != null && contains(exclude, name)) {
                    excludeFlag = true;
                    break;
                }
            }

            if (!runFlag || excludeFlag) {
                Transformers.invokeExact(original, frame);
            } else {
                if (frame.type().returnType() != void.class) {
                    frame.accessor().setValue(RETURN_VALUE_IDX, value);
                }
            }
        };
    }

    private static boolean contains(String[] array, String value) {
        if (array == null) return false;
        for (String s : array) { if (value.equals(s)) return true; }
        return false;
    }
}
