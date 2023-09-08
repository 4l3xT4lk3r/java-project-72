package hexlet.code;


import hexlet.code.domains.Url;
import hexlet.code.domains.UrlCheck;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hexlet.code.domains.query.QUrl;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class AppTest {

    private static Javalin app;
    private static String baseUrl;
    private static Database database;

    private static MockWebServer webServer;

    private static String webServerPage;

    @BeforeAll
    public static void beforeAll() throws Exception {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();

        webServer = new MockWebServer();
        webServer.start();
        webServer.enqueue(new MockResponse());
        webServerPage = webServer.url("/").toString().replaceAll(".$", "");
    }

    @AfterAll
    public static void afterAll() throws Exception {
        app.stop();
        webServer.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed.sql");
    }

    @Test
    public void testMainPage() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Анализатор страниц");
    }

    @Test
    public void testUrlPage() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Последняя Проверка");
    }

    @Test
    public void testCreateNewPage() {
        String page = "http://ya.ru";
        HttpResponse responsePost = Unirest.post(baseUrl + "/urls")
                .field("url", page)
                .asEmpty();
        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains(page);
        assertThat(body).contains("Страница успешно добавлена");

        Url url = new QUrl().name.equalTo(page).findOne();

        assertThat(url).isNotNull();
        assertThat(url.getName()).isEqualTo(page);
    }

    @Test
    public void testCreatePageAlreadyExist() {
        var url = new Url("http://ya.ru");
        url.save();
        String page = "http://ya.ru";
        HttpResponse responsePost = Unirest.post(baseUrl + "/urls")
                .field("url", page)
                .asEmpty();

        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains(page);
        assertThat(body).contains("Страница уже существует");
    }

    @Test
    public void testCreateWrongPage() {
        String page = "WrongUrlWrongUrl";
        HttpResponse responsePost = Unirest.post(baseUrl + "/urls")
                .field("url", page)
                .asString();
        assertThat(responsePost.getStatus()).isEqualTo(422);
        String body = responsePost.getBody().toString();
        assertThat(body).contains("Некорректный URL");
        Url url = new QUrl().name.equalTo(page).findOne();
        assertThat(url).isNull();
    }

    @Test
    public void testShowUrlById() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls/1").asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains("http://mail.ru");
        assertThat(body).contains("Дата создания");
    }

    @Test
    public void testCheckUrl() {
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/html; charset=utf-8")
                .setBody("""
                        <html><head><title>CISCO</title></head>
                        <body><h1>ROUTING</h1></body></html>""")
                .setResponseCode(200);
        webServer.enqueue(response);

        Unirest.post(baseUrl + "/urls").field("url", webServerPage).asEmpty();
        HttpResponse<String> getResponse = Unirest.get(baseUrl + "/urls").asString();
        String body = getResponse.getBody();
        assertThat(body).contains(webServerPage);

        HttpResponse postResponse = Unirest.post(baseUrl + "/urls/3/checks").asEmpty();
        assertThat(postResponse.getStatus()).isEqualTo(302);

        getResponse = Unirest.get(baseUrl + "/urls/3").asString();
        assertThat(getResponse.getStatus()).isEqualTo(200);
        body = getResponse.getBody();
        assertThat(body).contains(webServerPage);
        assertThat(body).contains("CISCO");
        assertThat(body).contains("ROUTING");

        Url url = new QUrl().name.equalTo(webServerPage).findOne();
        assertThat(url).isNotNull();

        List<UrlCheck> urlChecks = new QUrl().urlChecks.fetch().name.equalTo(webServerPage).findIds();
        assertThat(urlChecks.size()).isGreaterThan(0);
    }

}
