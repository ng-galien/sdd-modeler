package io.statemodeler.cli;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.repository.SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

class CliCommandHelpersTest {

    @Test
    void checkHashRejectsBlank() {
        var result = CliCommandHelpers.checkHash(" \t");
        assertTrue(result.isFailure());
        assertTrue(result.isFailure());
        assertTrue(result.getCause() instanceof IllegalArgumentException);
        assertTrue(result.getCause().getMessage().contains("Hash cannot be empty"));
    }

    @Test
    void findByHashReportsErrorWhenHashMissing() {
        var ctx = commandSpecWithCapturedErr();
        var repo = new StubRepository();

        CliCommandHelpers.findByHash(ctx.spec(), repo, "");

        assertTrue(ctx.err().toString().contains("Hash cannot be empty"));
        assertFalse(repo.findByHashCalled);
    }

    @Test
    void findByHashReportsWhenNotFound() {
        var ctx = commandSpecWithCapturedErr();
        var repo = new StubRepository();
        CliCommandHelpers.findByHash(ctx.spec(), repo, "abc");

        assertTrue(ctx.err().toString().contains("SDR not found"));
        assertTrue(repo.findByHashCalled);
    }

    @Test
    void deleteSdrFailsWhenRepositoryReturnsFalse() {
        var ctx = commandSpecWithCapturedErr();
        var repo = new StubRepository();
        var record = sampleRecord();
        repo.store(record);
        repo.deleteResult = Try.success(false);

        CliCommandHelpers.deleteSdr(ctx.spec(), repo, record);

        assertTrue(ctx.err().toString().contains("SDR not found during deletion"));
    }

    private record SpecCtx(CommandSpec spec, StringWriter err) {}

    private static SpecCtx commandSpecWithCapturedErr() {
        CommandSpec spec = CommandSpec.create();
        var cmd = new CommandLine(spec);
        var err = new StringWriter();
        cmd.setErr(new PrintWriter(err, true));
        spec.mixinStandardHelpOptions(true);
        return new SpecCtx(cmd.getCommandSpec(), err);
    }

    private static SdrRecord sampleRecord() {
        return new SdrRecord("{}", "application/json", "CREATE TABLE t();", "hash1", "hash2", "v1");
    }

    static class StubRepository implements SdrRepository {
        boolean findByHashCalled = false;
        Try<Boolean> deleteResult = Try.success(true);
        private Optional<SdrRecord> stored = Optional.empty();

        void store(SdrRecord record) {
            this.stored = Optional.ofNullable(record);
        }

        @Override
        public Try<Void> save(SdrRecord record, String name, String version) {
            store(record);
            return Try.success(null);
        }

        @Override
        public Try<Optional<SdrRecord>> findByHash(String hash) {
            findByHashCalled = true;
            return Try.success(stored);
        }

        @Override
        public Try<List<io.statemodeler.repository.SdrMetadata>> findByName(String modelName) {
            return Try.success(Collections.emptyList());
        }

        @Override
        public Try<Optional<SdrRecord>> findByNameAndVersion(String name, String version) {
            return Try.success(Optional.empty());
        }

        @Override
        public Try<List<io.statemodeler.repository.SdrMetadata>> listAll() {
            return Try.success(Collections.emptyList());
        }

        @Override
        public Try<List<io.statemodeler.repository.SdrMetadata>> findRecent(int limit) {
            return Try.success(Collections.emptyList());
        }

        @Override
        public Try<Boolean> delete(String hash) {
            return deleteResult;
        }

        @Override
        public Try<Long> count() {
            return Try.success(0L);
        }

        @Override
        public Try<Boolean> exists(String hash) {
            return Try.success(stored.map(r -> r.schemaHash().equals(hash)).orElse(false));
        }

        @Override
        public Try<Void> saveMigration(io.statemodeler.repository.SdrMigration migration) {
            return Try.success(null);
        }

        @Override
        public Try<Optional<io.statemodeler.repository.SdrMigration>> findMigration(String fromHash, String toHash) {
            return Try.success(Optional.empty());
        }

        @Override
        public Try<List<io.statemodeler.repository.SdrMigration>> findMigrationsFrom(String fromHash) {
            return Try.success(Collections.emptyList());
        }

        @Override
        public Try<List<io.statemodeler.repository.SdrMigration>> findMigrationsTo(String toHash) {
            return Try.success(Collections.emptyList());
        }

        @Override
        public Try<Boolean> deleteMigration(String fromHash, String toHash) {
            return Try.success(true);
        }

        @Override
        public void close() {
            // no-op
        }
    }
}
