package hexlet.code;

import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlController;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.get;

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

    private static String getJdbcUrl() {
        return System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project");
    }


    private static void setMode() {
        System.setProperty("APP_ENV", getMode());
    }

    private static void setJdbcUrl() {
        System.setProperty("JDBC_DATABASE_URL", getJdbcUrl());
    }

    private static boolean isProduction() {
        return getMode().equals("production");
    }

    public static void addRoutes(Javalin app) {
        app.get("/", RootController.welcome);
        app.routes(() -> {
            path("urls", () -> {
                get(UrlController.listUrls);
                post(UrlController.createUrl);
                path("{id}", () -> {
                    get(UrlController.showUrl);
                    path("checks", () -> {
                        post(UrlController.checkUrl);
                    });
                });
            });
        });
    }

    private static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setCharacterEncoding("UTF-8");

        templateEngine.addTemplateResolver(templateResolver);
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());

        return templateEngine;
    }

    public static Javalin getApp() {
        setMode();
        setJdbcUrl();
        Javalin app = Javalin.create(config -> {
            if (!isProduction()) {
                config.plugins.enableDevLogging();
            }
            JavalinThymeleaf.init(getTemplateEngine());
        });
        addRoutes(app);
        return app;
    }
}
