package hexlet.code;

import io.javalin.http.Context;

import java.net.URL;

public final class Utils {
    public static void removeFlashMessage(Context ctx) {
        ctx.sessionAttribute("flash", null);
    }

    public static String normalizeUrl(URL url) {
        return String
                .format(
                        "%s://%s%s",
                        url.getProtocol(),
                        url.getHost(),
                        url.getPort() == -1 ? "" : ":" + url.getPort()
                )
                .toLowerCase();
    }

}
