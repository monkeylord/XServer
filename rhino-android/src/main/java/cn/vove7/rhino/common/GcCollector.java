package cn.vove7.rhino.common;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by 17719247306.
 * Date: 2018/8/26
 */
public class GcCollector {

    public static final GcCollector Ins = new GcCollector();

    private static HashMap<Scriptable, RunData> colls = new HashMap<>();

    static class RunData {
        Thread oriThread;
        List<Object> gcs = new ArrayList<>();

        public RunData(Thread oriThread) {
            this.oriThread = oriThread;
        }
    }

    public static void regMainThread(Scriptable scope, Thread thread) {
        RunData data = new RunData(thread);
        colls.put(scope, data);
    }

    @JSFunction
    public static void reg(Scriptable scope, Object o) {
        if (colls.containsKey(scope)) {
            colls.get(scope).gcs.add(o);
        }
    }

    public static void gc(Scriptable scope) {
        RunData data = colls.get(scope);
        if (data == null) {
            System.out.println("no data");
            return;
        }
        colls.remove(scope);

        List<Object> gcs = data.gcs;
        if (gcs != null) {
            for (Object gc : gcs) {
                if (gc instanceof Thread && ((Thread) gc).isAlive()) {
                    ((Thread) gc).interrupt();
                }
            }
            gcs.clear();
        }
        Thread or = data.oriThread;
        if (or != null) {
            or.interrupt();
        }
    }
}
