package hexlet.code.models;

import java.time.Instant;


public final class UrlCheck {
    private long id;
    private int statusCode;
    private String title;
    private String h1;
    private String description;
    private long url_id;

    private Instant createdAt;

    public UrlCheck(int statusCode, String title, String h1, String description, long url_id) {
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
        this.url_id = url_id;
    }
    public UrlCheck(int statusCode, String title, String h1, String description, long url_id, Instant createdAt) {
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
        this.url_id = url_id;
        this.createdAt = createdAt;
    }

    public long getUrl_id() {
        return url_id;
    }

    public long getId() {
        return id;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getTitle() {
        return title;
    }

    public String getH1() {
        return h1;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(long id) {
        this.id = id;
    }
}
