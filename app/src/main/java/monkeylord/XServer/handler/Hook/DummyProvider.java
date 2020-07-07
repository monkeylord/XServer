package monkeylord.XServer.handler.Hook;

import java.lang.reflect.Member;

import monkeylord.XServer.handler.HookHandler;

// This is a dummy provider, if a hook framework cannot register its own class, it can hook and use this provider.
public class DummyProvider implements HookHandler.HookProvider {
    @Override
    public Unhook hookMethod(Member hookMethod, XServer_MethodHook callback) {
        return null;
    }

    @Override
    public void unhookMethod(Member hookMethod, Object additionalObj) {

    }
}
