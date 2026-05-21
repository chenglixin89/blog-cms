package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Bridges databases that were created before certain columns existed in V1.
 * Each ALTER is guarded by an information_schema lookup, so on a fresh install
 * (where V1 already contains every column) this migration is a no-op.
 *
 * <p>Mirrors the legacy {@code DatabaseMigrationRunner.addColumnIfMissing}
 * calls; we keep them as a Flyway migration so that any environment that ran
 * very old releases can still upgrade cleanly.</p>
 */
public class V2__add_legacy_missing_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        // blog_article: SEO fields (added later than the rest).
        addColumnIfMissing(connection, "blog_article", "seo_title",
            "VARCHAR(200) NOT NULL DEFAULT '' AFTER summary");
        addColumnIfMissing(connection, "blog_article", "seo_description",
            "VARCHAR(500) NOT NULL DEFAULT '' AFTER seo_title");
        addColumnIfMissing(connection, "blog_article", "seo_keywords",
            "VARCHAR(255) NOT NULL DEFAULT '' AFTER seo_description");

        // blog_user: profile fields.
        addColumnIfMissing(connection, "blog_user", "email",
            "VARCHAR(120) NOT NULL DEFAULT '' AFTER nickname");
        addColumnIfMissing(connection, "blog_user", "avatar",
            "VARCHAR(500) NOT NULL DEFAULT '' AFTER email");
        addColumnIfMissing(connection, "blog_user", "bio",
            "VARCHAR(500) NOT NULL DEFAULT '' AFTER avatar");
        addColumnIfMissing(connection, "blog_user", "last_login_at",
            "DATETIME NULL AFTER status");

        // blog_ai_call_log: full input/output capture fields.
        addColumnIfMissing(connection, "blog_ai_call_log", "input_text",
            "LONGTEXT NULL AFTER skill_name");
        addColumnIfMissing(connection, "blog_ai_call_log", "output_text",
            "LONGTEXT NULL AFTER input_text");
    }

    private void addColumnIfMissing(Connection connection, String table, String column, String definition) throws Exception {
        try (PreparedStatement check = connection.prepareStatement(
            "SELECT COUNT(*) FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?"
        )) {
            check.setString(1, table);
            check.setString(2, column);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        // Both identifiers are hardcoded constants from this migration; safe to interpolate.
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
