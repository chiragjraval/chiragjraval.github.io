package com.chirag.stockscreener.context;

import com.chirag.stockscreener.enums.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionContextImplTest {

    @Test
    void defaultsToSafeTestEnvironment() throws Exception {
        ExecutionContext context = new ExecutionContextImpl(new String[0]);

        assertEquals(Environment.TEST, context.getEnvironment());
        assertEquals("src/main/resources/screener-data/test", context.getOutputDirectory());
        assertEquals("src/main/resources/screener-data/test/", context.getGitFilePattern());
        assertFalse(context.isGitCommitEnabled());
    }

    @Test
    void acceptsExplicitProductionEnvironmentAndQuery() throws Exception {
        String queryLink = "https://www.screener.in/screens/123/example";
        ExecutionContext context = new ExecutionContextImpl(new String[]{
                "-e", "prod",
                "-sql", queryLink
        });

        assertEquals(Environment.PROD, context.getEnvironment());
        assertEquals(queryLink, context.getScreenerQueryLink());
        assertEquals("src/main/resources/screener-data/prod", context.getOutputDirectory());
        assertTrue(context.isGitCommitEnabled());
    }
}
