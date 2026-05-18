package com.shigui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaContractTest {
    @Test
    void initSchemaContainsSprint6ClaimColumnsAndActiveClaimGuard() throws Exception {
        String schema = Files.readString(Path.of("../scripts/init_schema.sql"));

        assertThat(schema).contains("PENDING_AI_REVIEW");
        assertThat(schema).contains("ai_decision");
        assertThat(schema).contains("ai_confidence");
        assertThat(schema).contains("ai_reason");
        assertThat(schema).contains("admin_reason");
        assertThat(schema).contains("verified_at");
        assertThat(schema).contains("completed_at");
        assertThat(schema).contains("active_claim_post_id");
        assertThat(schema).contains("uk_active_claim_post");
    }
}
