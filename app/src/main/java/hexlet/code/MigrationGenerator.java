package hexlet.code;


import io.ebean.dbmigration.DbMigration;
import io.ebean.annotation.Platform;

import java.io.IOException;

public final class MigrationGenerator {

    public static void main(String[] args) throws IOException {
        DbMigration dbMigration = DbMigration.create();
        dbMigration.addPlatform(Platform.H2, "h2");
        dbMigration.addPlatform(Platform.POSTGRES, "postgres");
        dbMigration.generateMigration();
    }
}