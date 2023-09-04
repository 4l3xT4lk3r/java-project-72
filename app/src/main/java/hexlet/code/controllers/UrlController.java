package hexlet.code.controllers;

import hexlet.code.domains.Url;
import hexlet.code.domains.query.QUrl;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.ebean.PagedList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class UrlController {
    public static Handler createUrl = ctx -> {
        try {
            new URL(ctx.formParam("url"));
            if (new QUrl().name.equalTo(removePathFromUrl(ctx.formParam("url"))).findOne() == null) {
                Url url = new Url(removePathFromUrl(ctx.formParam("url")));
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
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
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
        removeFlashMessage(ctx);
    };

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);
        Url url = new QUrl().id.equalTo(id).findOne();
        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    private static void removeFlashMessage(Context ctx) {
        ctx.sessionAttribute("flash", null);
    }

    private static String removePathFromUrl(String url) {
        Pattern pattern = Pattern.compile("https?://[^/]+");
        Matcher matcher = pattern.matcher(url);
        matcher.find();
        return matcher.group();
    }

}
