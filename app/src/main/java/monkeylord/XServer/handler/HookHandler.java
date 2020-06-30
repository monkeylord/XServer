package monkeylord.XServer.handler;

import monkeylord.XServer.handler.Hook.Unhook;
import monkeylord.XServer.handler.Hook.XServer_MethodHook;

//处理hook相关内容
public class HookHandler {
    static HookProvider provider = null;

    public static HookProvider getProvider() throws NullPointerException{
        if(provider==null)throw new NullPointerException("No provider available");
        return provider;
    }

    public static boolean setProvider(HookProvider newProvider){
        if(provider!=null || newProvider==null)return false;
        provider=newProvider;
        return true;
    }

    public interface HookProvider{
        Unhook hookMethod(java.lang.reflect.Member hookMethod, XServer_MethodHook callback);
        void unhookMethod(java.lang.reflect.Member hookMethod, Object additionalObj);
    }
}
