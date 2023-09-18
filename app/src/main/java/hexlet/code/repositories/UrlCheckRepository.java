package hexlet.code.repositories;

import hexlet.code.models.Url;
import hexlet.code.models.UrlCheck;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UrlCheckRepository extends BaseRepository {
    public static void save(UrlCheck urlCheck) throws SQLException {
        //UrlCheck urlCheck = new UrlCheck(code, title, h1, description, url);
        String sql = "INSERT INTO url_checks (status_code,title,h1,description,url_id) VALUES (?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, urlCheck.getStatusCode());
            preparedStatement.setString(2, urlCheck.getTitle());
            preparedStatement.setString(3, urlCheck.getH1());
            preparedStatement.setString(4, urlCheck.getDescription());
            preparedStatement.setLong(5, urlCheck.getUrl_id());
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                urlCheck.setId(generatedKeys.getLong(1));
            } else {
                throw new SQLException("DB have not returned an id after saving an entity");
            }
        }
    }

    public static List<UrlCheck> getEntities(long url_id) throws SQLException {
        String sql = "SELECT * FROM url_checks WHERE url_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, url_id);
            ResultSet resultSet = stmt.executeQuery();
            List<UrlCheck> result = new ArrayList<>();
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                int statusCode = resultSet.getInt("status_code");
                String title = resultSet.getString("title");
                String h1 = resultSet.getString("h1");
                String description = resultSet.getString("description");
                Instant createdAt = resultSet.getTimestamp("created_at").toInstant();

                UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url_id, createdAt);
                urlCheck.setId(id);
                result.add(urlCheck);
            }
            return result;
        }
    }
}
