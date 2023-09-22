package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlController;
import hexlet.code.repositories.BaseRepository;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.stream.Collectors;

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

    private static boolean isProduction() {
        return getMode().equals("production");
    }

    private static String getJdbcUrl() {
        return System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project");
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

    public static void dbInit() throws IOException, SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getJdbcUrl());
        BaseRepository.dataSource = new HikariDataSource(hikariConfig);
        //if (getJdbcUrl().equals("jdbc:h2:mem:project")) {
        try (var connection = BaseRepository.dataSource.getConnection();
             var statement = connection.createStatement()) {
            File file = new File("src/main/resources/schema.sql");
            String sql = Files.lines(file.toPath())
                    .collect(Collectors.joining("\n"));
            statement.execute(sql);
        }
        //}
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
        try {
            dbInit();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        Javalin app = Javalin.create(config -> {
            if (!isProduction()) {
                config.plugins.enableDevLogging();
            }
            JavalinThymeleaf.init(getTemplateEngine());
        });
        app.error(404, ctx -> {
            ctx.render("/404.html");
        });
        addRoutes(app);
        return app;
    }
}
