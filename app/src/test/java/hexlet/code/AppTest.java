package hexlet.code;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.models.Url;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
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
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        webServer = new MockWebServer();
        webServer.start();
        webServerPage = webServer.url("/").toString().replaceAll(".$", "");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:mem:project");
        dataSource = new HikariDataSource(hikariConfig);
    }

    public static Url findUrlByName(String name) {
        Url url = null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM urls WHERE name = ?")) {
            stmt.setString(1, name);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                long id = resultSet.getLong("id");
                Instant createdAt = resultSet.getTimestamp("created_at").toInstant();
                url = new Url(id, name, createdAt);
            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return url;
    }


    @AfterAll
    public static void afterAll() throws Exception {
        app.stop();
        webServer.shutdown();
        dataSource.close();
    }

    @BeforeEach
    void beforeEach() {

    }

    @Test
    public void testMainPage() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertEquals(200, response.getStatus());
        assertTrue(response.getBody().contains("Анализатор страниц"));
    }

    @Test
    public void testUrlPage() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        assertEquals(200, response.getStatus());
        assertTrue(response.getBody().contains("Последняя Проверка"));
    }

    @Test
    public void testCreateNewPage() throws SQLException {
        String page = "https://leetcode.com/";
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
        assertNotNull(findUrlByName(page));
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
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/html; charset=utf-8")
                .setBody("""
                        <html><head><title>CISCO</title></head>
                        <body><h1>ROUTING</h1></body></html>""")
                .setResponseCode(200);
        webServer.enqueue(response);

        Unirest.post(baseUrl + "/urls").field("url", webServerPage).asEmpty();
        HttpResponse<String> getResponse = Unirest.get(baseUrl + "/urls?page=3").asString();
        String body = getResponse.getBody();

        assertTrue(body.contains(webServerPage));
        Url url = findUrlByName(webServerPage);
        assertNotNull(url);

        HttpResponse postResponse = Unirest.post(baseUrl + "/urls/" + url.getId() + "/checks").asEmpty();
        assertEquals(302, postResponse.getStatus());

        getResponse = Unirest.get(baseUrl + "/urls/" + url.getId()).asString();
        assertEquals(200, getResponse.getStatus());
        body = getResponse.getBody();
        assertTrue(body.contains(webServerPage));
        assertTrue(body.contains("CISCO"));
        assertTrue(body.contains("ROUTING"));
    }
}
