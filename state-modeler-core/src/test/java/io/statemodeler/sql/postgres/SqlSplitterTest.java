package io.statemodeler.sql.postgres;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SqlSplitterTest {

    @Test
    void shouldSplitSemicolonsOutsideDollarQuotedBodies() {
        String ddl =
                "CREATE FUNCTION public.sync_test() RETURNS TRIGGER AS $fn_test$ BEGIN;\nRAISE NOTICE 'ok';\nEND;$fn_test$;\nCREATE TABLE public.test_table (id serial PRIMARY KEY);";

        var statements = io.statemodeler.sql.postgres.SqlSplitter.splitSqlStatements(ddl);

        assertEquals(2, statements.size(), "Should have split into two statements");
        assertTrue(statements.get(0).trim().startsWith("CREATE FUNCTION"));
        assertTrue(statements.get(1).trim().startsWith("CREATE TABLE"));
    }

    @Test
    void shouldHandleNestedDollars() {
        String ddl = "CREATE FUNCTION outer() AS $outer$ BEGIN RAISE NOTICE $inner$inner$inner$; END; $outer$;";
        var statements = io.statemodeler.sql.postgres.SqlSplitter.splitSqlStatements(ddl);
        assertEquals(1, statements.size());
    }

    @Test
    void shouldHandleDollarsWithSemicolons() {
        String ddl = "CREATE FUNCTION f() AS $$ BEGIN; END; $$; SELECT 1;";
        var statements = io.statemodeler.sql.postgres.SqlSplitter.splitSqlStatements(ddl);
        assertEquals(2, statements.size());
        assertEquals("CREATE FUNCTION f() AS $$ BEGIN; END; $$", statements.get(0));
        assertEquals("SELECT 1", statements.get(1));
    }
}
