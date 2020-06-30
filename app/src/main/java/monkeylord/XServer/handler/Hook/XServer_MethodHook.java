package monkeylord.XServer.handler.Hook;

public abstract class XServer_MethodHook {
    public void beforeHookedMethod(XServer_Param param) throws Throwable {}
    public void afterHookedMethod(XServer_Param param) throws Throwable {}
}
