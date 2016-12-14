package utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.wiltonwebtoolkit.WiltonException;
import net.wiltonwebtoolkit.WiltonGateway;

import java.util.Map;

import static net.wiltonwebtoolkit.WiltonJni.wiltoncall;
import static utils.TestUtils.*;

/**
 * User: alexkasko
 * Date: 6/15/16
 */
public class TestGateway implements WiltonGateway {

    public static final String HELLO_RESP = "Hello Java!\n";
    public static final String ASYNC_RESP = "Hello from Async\n";
    public static final String QUERIES_RESP = "{\n" +
            "  \"foo\": \"baa,bar\",\n" +
            "  \"boo\": \"baz\"\n" +
            "}";
    public static final int TCP_PORT = 8080;
    public static final int TCP_PORT_HTTPS = 8443;
    public static final String ROOT_URL = "http://127.0.0.1:" + TCP_PORT + "/";
    public static final String ROOT_URL_HTTPS = "https://localhost:" + TCP_PORT_HTTPS + "/";
    public static final String LOG_DATA = "Please append me to log";
    public static final String STATIC_FILE_DATA = "I am data from static file";
    public static final String STATIC_ZIP_DATA = "I am data from ZIP file";

    @SuppressWarnings("unchecked") // headers access
    @Override
    public void gatewayCallback(String callbackModule, long requestHandle) {
        try {
            String meta = wiltoncall("request_get_metadata", GSON.toJson(ImmutableMap.builder()
                    .put("requestHandle", requestHandle)
                    .build()));
            Map<String, Object> metaMap = GSON.fromJson(meta, MAP_TYPE);
            final String resp;
            if ("/hello".equalsIgnoreCase(callbackModule)) {
                resp = HELLO_RESP;
            } else if ("/headers".equalsIgnoreCase(callbackModule)) {
                wiltoncall("request_set_response_metadata", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .put("metadata", ImmutableMap.builder()
                                .put("headers", ImmutableMap.builder()
                                        .put("X-Server-H1", "foo")
                                        .put("X-Server-H2", "bar")
                                        .put("X-Proto", metaMap.get("protocol"))
                                        .build())
                                .build())
                        .build()));
                resp = GSON.toJson(metaMap.get("headers"));
            } else if ("/postmirror".equalsIgnoreCase(callbackModule)) {
                resp = wiltoncall("request_get_data", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .build()));
            } else if ("/querymirror".equalsIgnoreCase(callbackModule)) {
                resp = GSON.toJson(metaMap.get("queries"));
            } else if ("/logger".equalsIgnoreCase(callbackModule)) {
                String msg = wiltoncall("request_get_data", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .build()));
                String data = GSON.toJson(ImmutableMap.builder()
                        .put("level", "INFO")
                        .put("logger", TestGateway.class.getName())
                        .put("message", msg)
                        .build());
                wiltoncall("logger_log", data);
                resp = "";
            } else if ("/sendfile".equalsIgnoreCase(callbackModule)) {
                String filename = wiltoncall("request_get_data", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .build()));
                wiltoncall("request_send_temp_file", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .put("filePath", filename)
                        .build()));
                resp = null;
            } else if ("/mustache".equalsIgnoreCase(callbackModule)) {
                Map<String, String> headers = (Map<String, String>) metaMap.get("headers");
                String mustacheFile = headers.get("X-Mustache-File");
                String valuesJson = wiltoncall("request_get_data", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .build()));
                Map<String, Object> values = GSON.fromJson(valuesJson, MAP_TYPE);
                wiltoncall("request_send_mustache", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .put("mustacheFilePath", mustacheFile)
                        .put("values", values)
                        .build()));
                resp = null;
            } else if ("/async".equalsIgnoreCase(callbackModule)) {
                String out = wiltoncall("request_send_later", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .build()));
                Map<String, Long> hamap = GSON.fromJson(out, LONG_MAP_TYPE);
                final long responseWriterHandle = hamap.get("responseWriterHandle");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sleepQuietly(200);
                        wiltoncall("request_send_with_response_writer", GSON.toJson(ImmutableMap.builder()
                                .put("responseWriterHandle", responseWriterHandle)
                                .put("data", ASYNC_RESP)
                                .build()));
                    }
                }).start();
                resp = null;
            } else if ("/reqfilename".equalsIgnoreCase(callbackModule)) {
                resp = wiltoncall("request_get_data_filename", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .build()));
            } else {
                throw new WiltonException("Unregistered module name: [" + callbackModule + "]");
            }
            if (null != resp) { // sendfile case
                wiltoncall("request_send_response", GSON.toJson(ImmutableMap.builder()
                        .put("requestHandle", requestHandle)
                        .put("data", resp)
                        .build()));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            wiltoncall("request_send_response", GSON.toJson(ImmutableMap.builder()
                    .put("requestHandle", requestHandle)
                    .put("data", e.getMessage())
                    .build()));
        }
    }

    public static ImmutableList<ImmutableMap<String, String>> views() {
        return ImmutableList.<ImmutableMap<String, String>>builder()
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "GET")
                        .put("path", "/hello")
                        .put("module", "/hello")
                        .build())
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "GET")
                        .put("path", "/headers")
                        .put("module", "/headers")
                        .build())
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "POST")
                        .put("path", "/postmirror")
                        .put("module", "/postmirror")
                        .build())
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "GET")
                        .put("path", "/querymirror")
                        .put("module", "/querymirror")
                        .build())
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "POST")
                        .put("path", "/logger")
                        .put("module", "/logger")
                        .build())
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "POST")
                        .put("path", "/sendfile")
                        .put("module", "/sendfile")
                        .build())
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "POST")
                        .put("path", "/mustache")
                        .put("module", "/mustache")
                        .build())
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "GET")
                        .put("path", "/async")
                        .put("module", "/async")
                        .build())
                .add(ImmutableMap.<String, String>builder()
                        .put("method", "POST")
                        .put("path", "/reqfilename")
                        .put("module", "/reqfilename")
                        .build())
                .build();
    }

}
