package hexlet.code.controllers;

import hexlet.code.Utils;
import hexlet.code.models.Url;
import hexlet.code.models.UrlCheck;
import hexlet.code.repositories.UrlCheckRepository;
import hexlet.code.repositories.UrlRepository;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public final class UrlController {
    public static Handler createUrl = ctx -> {
        URL url;
        try {
            url = new URL(ctx.formParam("url"));
        } catch (MalformedURLException exception) {
            ctx.status(422);
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flashtype", "alert-danger");
            ctx.render("/index.html");
            return;
        }
        if (UrlRepository.find((ctx.formParam("url"))).isEmpty()) {
            String normalizedUrl = Utils.normalizeUrl(url);
            UrlRepository.save(new Url(normalizedUrl));
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flashtype", "alert-success");
        } else {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flashtype", "alert-warning");
        }
        ctx.redirect("/urls");
    };

    public static Handler listUrls = ctx -> {
        int page;
        try {
            page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        } catch (Exception exception) {
            page = 1;
        }
        int rowsPerPage = 10;
        int urlsCount = UrlRepository.getEntitiesCount();

        int lastPage = urlsCount / rowsPerPage;
        if (urlsCount % rowsPerPage > 0 || lastPage == 0) {
            lastPage++;
        }
        if (page < 1) {
            page = 1;
        }
        if (page > lastPage) {
            page = lastPage;
        }

        List<Url> urls = UrlRepository.getEntities(rowsPerPage, (page - 1) * 10);
        Map<Url, UrlCheck> urlChecks = new HashMap<>();

        for (Url u : urls) {
            Optional<UrlCheck> urlCheck = UrlCheckRepository.findLastCheck(u.getId());
            if (urlCheck.isPresent()) {
                urlChecks.put(u, urlCheck.get());
            }
        }
        ctx.attribute("urls", urls);
        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("currentPage", page);
        ctx.attribute("lastPage", lastPage);
        ctx.render("urls/index.html");
        Utils.removeFlashMessage(ctx);
    };

    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = UrlRepository.find(id).get();
        List<UrlCheck> urlChecks = UrlCheckRepository.getEntities(id);

        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("url", url);
        ctx.render("urls/show.html");
        Utils.removeFlashMessage(ctx);
    };

    public static Handler checkUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);
        Url url = UrlRepository.find(id).get();

        try {
            HttpResponse<String> response = Unirest.get(url.getName()).asString();
            int code = response.getStatus();
            String body = response.getBody();
            Document document = Jsoup.parse(body);
            String title = document.title();

            Element element = document.selectFirst("h1");
            String h1 = (!Objects.isNull(element)) ? element.ownText() : "no data";

            element = document.selectFirst("meta[name=description]");
            String description = (!Objects.isNull(element)) ? element.attr("content") : "no data";

            UrlCheck urlCheck = new UrlCheck(code, title, h1, description, id);
            UrlCheckRepository.save(urlCheck);

            ctx.attribute("url", url);
            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flashtype", "alert-success");
            ctx.redirect("/urls/" + id);
        } catch (Exception exception) {
            ctx.sessionAttribute("flash", "Не удалось проверить страницу");
            ctx.sessionAttribute("flashtype", "alert-danger");
            ctx.redirect("/urls/" + id);
        }
    };

}
