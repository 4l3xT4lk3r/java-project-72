package hexlet.code;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.models.Url;
import hexlet.code.models.UrlCheck;
import hexlet.code.repositories.BaseRepository;
import hexlet.code.repositories.UrlCheckRepository;
import hexlet.code.repositories.UrlRepository;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Collectors;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class AppTest {

    private static Javalin app;
    private static String baseUrl;

    private static MockWebServer webServer;

    private static HikariDataSource dataSource;

    private static String webServerPage;

    @BeforeAll
    public static void beforeAll() throws Exception {
        System.setProperty("APP_ENV", "development");
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        webServer = new MockWebServer();
        webServer.start();
        webServerPage = webServer.url("/").toString().replaceAll(".$", "");
        MockResponse response = new MockResponse()
                .setBody(Files.readString(Path.of("src/test/resources/fixtures/index.html")))
                .setResponseCode(200);
        webServer.enqueue(response);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project"));

        dataSource = new HikariDataSource(hikariConfig);
        try (var connection = BaseRepository.dataSource.getConnection();
             var statement = connection.createStatement()) {
            URL schema = App.class.getClassLoader().getResource("seed.sql");
            String sql = Files.lines(new File(schema.getFile()).toPath())
                    .collect(Collectors.joining("\n"));
            statement.execute(sql);
        }
    }

    @AfterAll
    public static void afterAll() throws Exception {
        app.stop();
        webServer.shutdown();
        dataSource.close();
    }

    @Test
    public void testMainPage() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertEquals(200, response.getStatus());
        assertTrue(response.getBody().contains("Анализатор страниц"));
    }

    @Test
    public void testUrlPage() throws SQLException {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        assertEquals(200, response.getStatus());
        Url url = UrlRepository.find("https://ya.ru").get();
        assertTrue(response.getBody().contains(url.getName()));
    }

    @Test
    public void testCreateNewPage() throws SQLException {
        String page = "https://leetcode.com";
        HttpResponse responsePost = Unirest.post(baseUrl + "/urls")
                .field("url", page)
                .asEmpty();
        assertEquals(302, responsePost.getStatus());
        assertTrue(responsePost.getHeaders().getFirst("Location").equals("/urls"));

        HttpResponse<String> response = Unirest.get(baseUrl + "/urls?page=3").asString();
        String body = response.getBody();

        assertEquals(200, response.getStatus());

        assertTrue(body.contains(page));
        assertTrue(body.contains("Страница успешно добавлена"));
        assertNotNull(UrlRepository.find(page).get());
    }

    @Test
    public void testCreatePageAlreadyExist() {
        String page = "https://ya.ru";
        Unirest.post(baseUrl + "/urls")
                .field("url", page)
                .asEmpty();
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        String body = response.getBody();
        assertEquals(200, response.getStatus());
        assertTrue(body.contains(page));
        assertTrue(body.contains("Страница уже существует"));
    }

    @Test
    public void testCreateWrongPage() throws SQLException {
        String page = "WrongUrlWrongUrl";
        HttpResponse responsePost = Unirest.post(baseUrl + "/urls")
                .field("url", page)
                .asString();
        assertEquals(422, responsePost.getStatus());
        String body = responsePost.getBody().toString();
        assertTrue(body.contains("Некорректный URL"));

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM urls WHERE name = ?")) {
            stmt.setString(1, page);
            ResultSet resultSet = stmt.executeQuery();
            assertFalse(resultSet.next());
        }
    }

    @Test
    public void testShowUrlById() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls/21").asString();
        String body = response.getBody();
        assertTrue(body.contains("https://github.com"));
        assertTrue(body.contains("Дата создания"));
    }

    @Test
    public void testCheckUrl() throws SQLException {
        Unirest.post(baseUrl + "/urls").field("url", webServerPage).asEmpty();
        HttpResponse<String> getResponse = Unirest.get(baseUrl + "/urls?page=3").asString();
        String body = getResponse.getBody();

        assertTrue(body.contains(webServerPage));
        Url url = UrlRepository.find(webServerPage).get();
        assertNotNull(url);

        HttpResponse postResponse = Unirest.post(baseUrl + "/urls/" + url.getId() + "/checks").asEmpty();
        assertEquals(302, postResponse.getStatus());

        getResponse = Unirest.get(baseUrl + "/urls/" + url.getId()).asString();
        assertEquals(200, getResponse.getStatus());

        UrlCheck urlCheck = UrlCheckRepository.findLastCheck(url.getId()).get();
        assertNotNull(urlCheck);
        assertEquals("ROUTING", urlCheck.getH1());
        assertEquals("CISCO", urlCheck.getTitle());
        assertEquals("Multinational digital communications technology conglomerate", urlCheck.getDescription());
    }
}
