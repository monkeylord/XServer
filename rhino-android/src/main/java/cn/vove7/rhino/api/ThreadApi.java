package cn.vove7.rhino.api;

import org.mozilla.javascript.Scriptable;

import cn.vove7.rhino.common.GcCollector;

/**
 * Created by Vove on 2018/8/28
 */
public class ThreadApi extends AbsApi {
    @Override
    protected String[] funs() {
        return new String[0];
    }

    public static Thread start(Scriptable scrop, Runnable runnable) {
        Thread t = new WrappedThread(runnable);
        t.start();
        GcCollector.reg(scrop, t);
        return t;
    }
}
