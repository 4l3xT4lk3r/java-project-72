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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Objects;

public final class UrlController {
    public static Handler createUrl = ctx -> {
        try {
            new URL(ctx.formParam("url"));
            if (UrlRepository.find((ctx.formParam("url"))).isEmpty()) {
                Url url = new Url(Utils.removePathFromUrl(ctx.formParam("url")));
                UrlRepository.save(new Url((ctx.formParam("url"))));
                ctx.sessionAttribute("flash", "Страница успешно добавлена");
                ctx.sessionAttribute("flashtype", "alert-success");
            } else {
                ctx.sessionAttribute("flash", "Страница уже существует");
                ctx.sessionAttribute("flashtype", "alert-warning");
            }
            ctx.redirect("/urls");
        } catch (MalformedURLException exception) {
            ctx.status(422);
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flashtype", "alert-danger");
            ctx.render("/index.html");
        }
    };

    public static Handler listUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        List<Url> urls = UrlRepository.getEntities(rowsPerPage, page);
        
        int lastPage = urls.size() + 1;
        int currentPage = page;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
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
