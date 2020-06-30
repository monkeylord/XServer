package monkeylord.XServer.handler.Hook;

import monkeylord.XServer.handler.HookHandler;

public class Unhook {
    private final java.lang.reflect.Member hookMethod;
    private final Object additionalObj;

    public Unhook(java.lang.reflect.Member hookMethod, Object additionalObj) {
        this.hookMethod = hookMethod;
        this.additionalObj = additionalObj;
    }
    public void unhook() {
        HookHandler.getProvider().unhookMethod(this.hookMethod, this.additionalObj);
    }
}
