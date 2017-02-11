package net.wiltonwebtoolkit.support.rhino;

import net.wiltonwebtoolkit.WiltonException;
import net.wiltonwebtoolkit.WiltonGateway;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * User: alexkasko
 * Date: 2/11/17
 */
class WiltonRhinoGateway implements WiltonGateway {

    @Override
    public String runScript(String callbackScriptJson) throws Exception {
        WiltonRhinoEnvironment.checkInitialized();
        Scriptable scope = WiltonRhinoEnvironment.globalScope();
        Context cx = Context.enter();
        try {
            Object funObj = scope.get("runScript", scope);
            if (funObj instanceof Function) {
                Object args[] = {callbackScriptJson};
                Function fun = (Function) funObj;
                Object result = fun.call(cx, scope, scope, args);
                return Context.toString(result);
            } else {
                throw new WiltonException("Cannot access 'runScript' function in global Rhino scope," +
                        " call data: [" + callbackScriptJson + "]");
            }
        } finally {
            Context.exit();
        }
    }
}
