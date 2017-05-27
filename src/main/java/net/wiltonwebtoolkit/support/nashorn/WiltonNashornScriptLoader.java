package net.wiltonwebtoolkit.support.nashorn;

import jdk.nashorn.api.scripting.AbstractJSObject;
import net.wiltonwebtoolkit.WiltonException;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.io.File;
import java.util.Arrays;

import static net.wiltonwebtoolkit.WiltonJni.wiltoncall;
import static net.wiltonwebtoolkit.support.common.Utils.readFileToString;

/**
 * User: alexkasko
 * Date: 2/11/17
 */
public class WiltonNashornScriptLoader extends AbstractJSObject {

    private final ScriptEngine engine;
    private final ScriptContext context;

    public WiltonNashornScriptLoader(ScriptEngine engine, ScriptContext context) {
        this.engine = engine;
        this.context = context;
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public Object call(Object thiz, Object... args) {
        try {
            for (Object arg : args) {
                String filePath = String.valueOf(arg);
                File file = new File(filePath);
                String path = file.getAbsolutePath();
                String sourceCode = wiltoncall("fs_read_script_file_or_module", path);
                StringBuilder wrapper = new StringBuilder();
                // todo: check why e.stack is undefined in WILTON_run
//                wrapper.append("try {")
//                        .append(sourceCode)
//                        .append("\n} catch(e) {\n")
//                        .append("throw new Error(e.stack);\n")
//                        .append("}\n")
//                        .append("//# sourceURL=")
//                        .append(path);
                wrapper.append(sourceCode)
                        // see https://bugs.openjdk.java.net/browse/JDK-8032068
                        .append("\n//# sourceURL=")
                        .append(path);
                engine.eval(wrapper.toString(), context);
            }
            return null;
        } catch (Exception e) {
//            e.printStackTrace();
            throw new WiltonException("Error loading script: " + Arrays.toString(args) +
                    "\n" + e.getMessage(), e);
        }
    }

}
