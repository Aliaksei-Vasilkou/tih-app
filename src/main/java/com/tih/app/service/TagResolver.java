package com.tih.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tih.app.dto.LevelFilter;
import com.tih.app.model.QuestionLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * Resolves a raw tag string to a {@link LevelFilter} for cumulative level filtering.
 * <p>
 * Selecting a level includes that level and all levels below it, plus untagged questions
 * Non-level tags (e.g. "ACID") are treated as a no-op — {@code null} is returned.
 */
@Component
@Slf4j
public class TagResolver {

    // Built once at class-load time from the enum — adding a new level only requires updating QuestionLevel.
    private static final List<String> ALL_LEVEL_TAGS;

    static {
        List<String> tags = new ArrayList<>();

        for (QuestionLevel level : QuestionLevel.values()) {
            tags.add(level.getTag());
        }

        ALL_LEVEL_TAGS = List.copyOf(tags);
    }

    public LevelFilter resolve(String tag) {
        if (tag == null) {
            return null;
        }

        QuestionLevel requestedLevel = findLevel(tag);

        if (requestedLevel == null) {
            log.debug("Tag '{}' is not a recognised level tag — level filter not applied", tag);

            return null;
        }

        List<String> includedLevels = new ArrayList<>();

        for (QuestionLevel level : QuestionLevel.values()) {
            if (level.ordinal() <= requestedLevel.ordinal()) {
                includedLevels.add(level.getTag());
            }
        }

        return new LevelFilter(List.copyOf(includedLevels), ALL_LEVEL_TAGS);
    }

    private QuestionLevel findLevel(String tag) {
        for (QuestionLevel level : QuestionLevel.values()) {
            if (level.getTag().equals(tag)) {
                return level;
            }
        }

        return null;
    }
}
