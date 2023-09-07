package hexlet.code.domains;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import java.time.Instant;

@Entity
@Table(name = "url_checks")
public final class UrlCheck extends Model {
    @Id
    private int id;
    private int statusCode;
    private String title;
    private String h1;
    @Lob
    private String description;
    @ManyToOne
    @JoinColumn(name = "url_id", referencedColumnName = "id")
    private Url url;
    @WhenCreated
    private Instant createdAt;

    public UrlCheck(int statusCode, String title, String h1, String description, Url url) {
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
        this.url = url;
    }

    public int getId() {
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
}
