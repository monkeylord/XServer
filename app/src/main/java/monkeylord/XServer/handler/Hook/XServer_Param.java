package monkeylord.XServer.handler.Hook;

public class XServer_Param {
    // Copied from Xposed MethodHookParam
    public java.lang.Object[] args;
    public java.lang.reflect.Member method;
    public java.lang.Object result = null;
    public boolean returnEarly = false;
    public java.lang.Object thisObject;
    public java.lang.Throwable throwable = null;

    public java.lang.Object getResult() {
        return this.result;
    }

    public void setResult(java.lang.Object result) {
        this.result = result;
        this.throwable = null;
        this.returnEarly = true;
    }

    public java.lang.Throwable getThrowable() {
        return this.throwable;
    }

    public boolean hasThrowable() {
        return this.throwable != null;
    }

    public void setThrowable(java.lang.Throwable throwable) {
        this.throwable = throwable;
        this.result = null;
        this.returnEarly = true;
    }

    public java.lang.Object getResultOrThrowable() throws java.lang.Throwable {
        if (this.throwable == null) {
            return this.result;
        }
        throw this.throwable;
    }

}
