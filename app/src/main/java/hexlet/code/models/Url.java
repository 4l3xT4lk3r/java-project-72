package hexlet.code.models;


import java.time.Instant;

public final class Url {
    private long id;
    private String name;
    private Instant createdAt;
    public Url(String name) {
        this.name = name;
    }
    public Url(long id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
}
