package com.tih.app.dto;

import java.util.List;

/**
 * Resolved level filter produced by {@link com.tih.app.service.TagResolver}.
 *
 * @param includedLevels level tags that qualify (e.g. ["L1","L2"] for a ?tag=L2 request)
 * @param allLevelTags   the complete set of level tags — used for the "no level tag" clause
 */
public record LevelFilter(List<String> includedLevels, List<String> allLevelTags) {

}
