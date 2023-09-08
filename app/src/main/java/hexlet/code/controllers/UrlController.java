package hexlet.code.controllers;

import hexlet.code.Utils;
import hexlet.code.domains.Url;
import hexlet.code.domains.UrlCheck;
import hexlet.code.domains.query.QUrl;
import hexlet.code.domains.query.QUrlCheck;
import io.javalin.http.Handler;
import io.ebean.PagedList;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class UrlController {
    public static Handler createUrl = ctx -> {
        try {
            new URL(ctx.formParam("url"));
            if (new QUrl().name.equalTo(Utils.removePathFromUrl(ctx.formParam("url"))).findOne() == null) {
                Url url = new Url(Utils.removePathFromUrl(ctx.formParam("url")));
                url.save();
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

        PagedList<Url> pagedUrls = new QUrl()
                .urlChecks
                .fetch(QUrlCheck.alias().url).setMaxRows(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id
                .asc()
                .select(QUrl.alias().id, QUrl.alias().name, QUrlCheck.alias().createdAt, QUrlCheck.alias().statusCode)
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
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
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);
        Url url = new QUrl().id.equalTo(id).findOne();
        List<UrlCheck> urlChecks = new QUrlCheck().url
                .equalTo(url)
                .orderBy()
                .id
                .desc()
                .findList();
        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("url", url);
        ctx.render("urls/show.html");
        Utils.removeFlashMessage(ctx);
    };

    public static Handler checkUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);
        Url url = new QUrl().id.equalTo(id).findOne();

        try {
            Unirest.head(url.getName()).asString();
            HttpResponse<String> response = Unirest.get(url.getName()).asString();

            int code = response.getStatus();
            String body = response.getBody();
            String title = Utils.getBodyData(body, "(?i)<title[^>]*>(.*)</title>");
            String description = Utils.getBodyData(body, "(?i)<meta name=\\\"description\\\" content=\\\"([^\\\"]*)");
            String h1 = Utils.getBodyData(body, "(?i)<h1[^>]*>([^<]+)");

            UrlCheck urlCheck = new UrlCheck(code, title, h1, description, url);
            urlCheck.save();

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
