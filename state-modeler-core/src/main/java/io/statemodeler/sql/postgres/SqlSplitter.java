package io.statemodeler.sql.postgres;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for safely splitting SQL DDL statements while respecting dollar-quoted
 * function bodies (e.g., AS $tag$ ... $tag$).
 */
public final class SqlSplitter {

    private SqlSplitter() {}

    public static List<String> splitSqlStatements(String ddl) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDollar = false;
        String dollarTag = null;
        for (int i = 0; i < ddl.length(); i++) {
            char c = ddl.charAt(i);
            if (c == '$') {
                // Potential start/end of dollar-quoted tag
                int j = ddl.indexOf('$', i + 1);
                if (j > -1) {
                    String tag = ddl.substring(i, j + 1);
                    if (!inDollar) {
                        inDollar = true;
                        dollarTag = tag;
                        current.append(tag);
                        i = j;
                        continue;
                    } else if (tag.equals(dollarTag)) {
                        inDollar = false;
                        dollarTag = null;
                        current.append(tag);
                        i = j;
                        continue;
                    }
                }
            }
            if (c == ';' && !inDollar) {
                statements.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            String s = current.toString().trim();
            if (!s.isBlank()) statements.add(s);
        }
        return statements;
    }
}
