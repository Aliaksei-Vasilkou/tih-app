package com.tih.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tih.app.dto.LevelFilter;

class TagResolverTest {

    private static final String JUNIOR_LEVEL = "L1";
    private static final String MIDDLE_LEVEL = "L2";
    private static final String SENIOR_LEVEL = "L3";
    private static final String LEAD_LEVEL = "L4";
    private static final String LOWER_CASE_MIDDLE_LEVEL = "l2";

    private final TagResolver tagResolver = new TagResolver();

    @Test
    void resolve_nullTag_returnsNull() {
        // when
        LevelFilter result = tagResolver.resolve(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolve_nonLevelTag_returnsNull() {
        // when
        LevelFilter result = tagResolver.resolve("ACID");

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolve_l1Tag_returnsOnlyL1InIncludedLevels() {
        // when
        LevelFilter result = tagResolver.resolve(JUNIOR_LEVEL);

        // then
        assertThat(result).isNotNull();
        assertThat(result.includedLevels()).containsExactly(JUNIOR_LEVEL);
        assertThat(result.allLevelTags()).containsExactly(JUNIOR_LEVEL, MIDDLE_LEVEL, SENIOR_LEVEL, LEAD_LEVEL);
    }

    @Test
    void resolve_l2Tag_includesL1AndL2() {
        // when
        LevelFilter result = tagResolver.resolve(MIDDLE_LEVEL);

        // then
        assertThat(result).isNotNull();
        assertThat(result.includedLevels()).containsExactly(JUNIOR_LEVEL, MIDDLE_LEVEL);
        assertThat(result.allLevelTags()).containsExactly(JUNIOR_LEVEL, MIDDLE_LEVEL, SENIOR_LEVEL, LEAD_LEVEL);
    }

    @Test
    void resolve_l3Tag_includesL1ThroughL3() {
        // when
        LevelFilter result = tagResolver.resolve(SENIOR_LEVEL);

        // then
        assertThat(result).isNotNull();
        assertThat(result.includedLevels()).containsExactly(JUNIOR_LEVEL, MIDDLE_LEVEL, SENIOR_LEVEL);
        assertThat(result.allLevelTags()).containsExactly(JUNIOR_LEVEL, MIDDLE_LEVEL, SENIOR_LEVEL, LEAD_LEVEL);
    }

    @Test
    void resolve_l4Tag_includesAllLevels() {
        // when
        LevelFilter result = tagResolver.resolve(LEAD_LEVEL);

        // then
        assertThat(result).isNotNull();
        assertThat(result.includedLevels()).containsExactly(JUNIOR_LEVEL, MIDDLE_LEVEL, SENIOR_LEVEL, LEAD_LEVEL);
        assertThat(result.allLevelTags()).containsExactly(JUNIOR_LEVEL, MIDDLE_LEVEL, SENIOR_LEVEL, LEAD_LEVEL);
    }

    @Test
    void resolve_allLevelTagsIsAlwaysFullSet_forAnyLevelInput() {
        // given
        LevelFilter l1 = tagResolver.resolve(JUNIOR_LEVEL);
        LevelFilter l2 = tagResolver.resolve(MIDDLE_LEVEL);
        LevelFilter l3 = tagResolver.resolve(SENIOR_LEVEL);
        LevelFilter l4 = tagResolver.resolve(LEAD_LEVEL);

        // when - then
        assertThat(l1.allLevelTags()).isEqualTo(l2.allLevelTags());
        assertThat(l2.allLevelTags()).isEqualTo(l3.allLevelTags());
        assertThat(l3.allLevelTags()).isEqualTo(l4.allLevelTags());
        assertThat(l4.allLevelTags()).containsExactly(JUNIOR_LEVEL, MIDDLE_LEVEL, SENIOR_LEVEL, LEAD_LEVEL);
    }

    @Test
    void resolve_emptyString_returnsNull() {
        // when
        LevelFilter result = tagResolver.resolve("");

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolve_lowercaseLevelTag_returnsNull() {
        // given — tags are case-sensitive; "l2" is not a recognised level tag
        // when
        LevelFilter result = tagResolver.resolve(LOWER_CASE_MIDDLE_LEVEL);

        // then
        assertThat(result).isNull();
    }
}
