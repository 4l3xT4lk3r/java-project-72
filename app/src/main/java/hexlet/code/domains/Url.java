package hexlet.code.domains;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.Instant;
import java.util.List;


@Entity
@Table(name = "urls")
public final class Url extends Model {
    @Id
    private long id;
    private String name;
    @WhenCreated
    private Instant createdAt;
    @OneToMany(mappedBy = "url")
    private List<UrlCheck> urlChecks;
    public Url(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<UrlCheck> getUrlChecks() {
        return urlChecks;
    }
}
