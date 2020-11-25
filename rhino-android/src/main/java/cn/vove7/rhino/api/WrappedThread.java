

package cn.vove7.rhino.api;


import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import org.mozilla.javascript.WrappedException;

/**
 * Handy class for starting a new thread that has a looper. The looper can then be
 * used to create handler classes. Note that start() must still be called.
 */
public class WrappedThread extends Thread {

    Looper mLooper;
    private @Nullable
    Handler mHandler;
    Runnable runnable;

    public WrappedThread(String name) {
        super(name);

    }

    public WrappedThread(Runnable runnable) {
        this.runnable = runnable;
    }

    /**
     * Call back method that can be explicitly overridden if needed to execute some
     * setup before Looper loops.
     */
    protected void onLooperPrepared() {
        mHandler = new Handler(getLooper());
        mHandler.post(runnable);
    }

    @Override
    public void run() {
        //mTid = Process.myTid();
        Looper.prepare();

        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
        }
        onLooperPrepared();
        try {
            Looper.loop();
        } catch (WrappedException ie) {
            Throwable e = ie.getWrappedException();
            if (!(e instanceof InterruptedException)) {
                RhinoApi.onException(e);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            RhinoApi.onException(e);
        }

    }

    /**
     * This method returns the Looper associated with this thread. If this thread not been started
     * or for any reason isAlive() returns false, this method will return null. If this thread
     * has been started, this method will block until the looper has been initialized.
     *
     * @return The looper.
     */
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }

        // If the thread has been started, wait until the looper has been created.
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mLooper;
    }


    public Handler getThreadHandler() {
        if (mHandler == null) {
            mHandler = new Handler(getLooper());
        }
        return mHandler;
    }


    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }


    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }


}
