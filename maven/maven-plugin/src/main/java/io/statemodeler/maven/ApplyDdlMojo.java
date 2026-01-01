package io.statemodeler.maven;

import io.statemodeler.sql.DdlGenerators;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "apply-ddl", threadSafe = true)
public class ApplyDdlMojo extends AbstractSddMojo {

    @Parameter(property = "sdd.dbUrl")
    private String dbUrl;

    @Parameter(property = "sdd.dbHost", defaultValue = "${env.POSTGRES_HOST}")
    private String dbHost;

    @Parameter(property = "sdd.dbPort", defaultValue = "${env.POSTGRES_PORT}")
    private String dbPort;

    @Parameter(property = "sdd.dbName", defaultValue = "${env.POSTGRES_DB}")
    private String dbName;

    @Parameter(property = "sdd.dbUser", defaultValue = "${env.POSTGRES_USER}")
    private String dbUser;

    @Parameter(property = "sdd.dbPassword", defaultValue = "${env.POSTGRES_PASSWORD}")
    private String dbPassword;

    @Parameter(property = "sdd.schema", defaultValue = "public")
    private String schema;

    @Parameter(property = "sdd.currentSchema", defaultValue = "public_states")
    private String stateSchema;

    @Override
    public void execute() throws MojoExecutionException {
        var model = loadValidatedModel();
        var generator = DdlGenerators.forDialect(model.database().dialect());
        var ddl = generator.generateFormattedDdl(model);

        try (var conn = openConnection()) {
            if (liquibase()) {
                applyViaLiquibase(conn, ddl);
            } else {
                applyViaSql(conn, ddl);
            }
        } catch (SQLException | IOException e) {
            throw new MojoExecutionException("Failed to apply DDL: " + e.getMessage(), e);
        }
    }

    private Connection openConnection() throws SQLException, MojoExecutionException {
        String url = dbUrl;
        if (url == null || url.isBlank()) {
            String host = defaulted(dbHost, "localhost");
            String port = defaulted(dbPort, "5432");
            String name = defaulted(dbName, "sdd_test");
            url = "jdbc:postgresql://" + host + ":" + port + "/" + name + "?currentSchema=" + schema + "," + stateSchema;
        }
        var user = defaulted(dbUser, "test");
        var pass = defaulted(dbPassword, "test");
        getLog().info("Applying DDL to " + url + " as " + user);
        return DriverManager.getConnection(url, user, pass);
    }

    private void applyViaLiquibase(Connection connection, String ddl) throws IOException, MojoExecutionException {
        var ddlDir = ddlOutputDir();
        Files.createDirectories(ddlDir);
        var changelog = ddlDir.resolve("changelog.yaml");
        Files.writeString(changelog, LiquibaseYamlRenderer.render(ddl));

        try {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var accessor = new DirectoryResourceAccessor(ddlDir)) {
                new Liquibase(changelog.getFileName().toString(), accessor, database).update();
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Liquibase failed: " + e.getMessage(), e);
        }
    }

    private void applyViaSql(Connection connection, String ddl) throws SQLException {
        List<String> statements = Arrays.stream(ddl.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        connection.setAutoCommit(false);
        try (var stmt = connection.createStatement()) {
            for (String sql : statements) {
                stmt.addBatch(sql);
            }
            stmt.executeBatch();
            connection.commit();
        }
    }

    private String defaulted(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
