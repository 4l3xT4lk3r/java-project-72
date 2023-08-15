package hexlet.code;

import io.javalin.Javalin;

public class App {

    public static void main(String[] args) {
        getApp().start(getPort());
    }

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "8080");
        return Integer.valueOf(port);
    }
    private static String getMode() {
        return System.getenv().getOrDefault("APP_ENV", "development");
    }

    private static boolean isProduction() {
        return getMode().equals("production");
    }

    public static void  addRoutes(Javalin app){
        app.get("/", ctx -> ctx.result("Hello World"));
    }
    public static Javalin getApp(){
        Javalin app = Javalin.create(config -> {
            if (!isProduction()) {
                config.plugins.enableDevLogging();
            }
        });

        addRoutes(app);
        return app;
    }
}
