package hexlet.code;

import io.javalin.http.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {
    public static void removeFlashMessage(Context ctx) {
        ctx.sessionAttribute("flash", null);
    }

    public static String removePathFromUrl(String url) {
        Pattern pattern = Pattern.compile("https?://[^/]+");
        Matcher matcher = pattern.matcher(url);
        matcher.find();
        return matcher.group();
    }

}