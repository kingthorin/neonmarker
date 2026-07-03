/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zaproxy.zap.extension.neonmarker;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class NeonmarkerColorService {

    private static final Logger LOGGER = LogManager.getLogger(NeonmarkerColorService.class);

    static final Color PLACEHOLDER = new Color(0, 0, 0, 0);

    private static List<Color> palette;

    private final ArrayList<ColorMapping> colorRules = new ArrayList<>();
    private final Predicate<String> tagValidator;
    private final Runnable onChange;

    NeonmarkerColorService(Predicate<String> tagValidator, Runnable onChange) {
        this.tagValidator = tagValidator;
        this.onChange = onChange;
    }

    static List<Color> getPalette() {
        if (palette == null) {
            palette = new ArrayList<>(17);
            palette.addAll(
                    List.of(
                            // RAINBOW HACKER THEME
                            new Color(0xff8080),
                            new Color(0xffc080),
                            new Color(0xffff80),
                            new Color(0xc0ff80),
                            new Color(0x80ff80),
                            new Color(0x80ffc0),
                            new Color(0x80ffff),
                            new Color(0x80c0ff),
                            new Color(0x8080ff),
                            new Color(0xc080ff),
                            new Color(0xff80ff),
                            new Color(0xff80c0),
                            // CORPORATE EDITION
                            new Color(0xe0ffff),
                            new Color(0xa8c0c0),
                            new Color(0x708080),
                            new Color(0x384040),
                            // Placeholder
                            PLACEHOLDER));
        }
        return palette;
    }

    static void addToPalette(Color addColor) {
        getPalette().add(addColor);
    }

    List<ColorMapping> getColorRules() {
        return Collections.unmodifiableList(colorRules);
    }

    void addEmptyRule() {
        colorRules.add(new ColorMapping());
        onChange.run();
    }

    void clearRules() {
        colorRules.clear();
        colorRules.add(new ColorMapping());
        onChange.run();
    }

    void swapRules(int ruleIndex, boolean up) {
        int other = up ? ruleIndex - 1 : ruleIndex + 1;
        if (ruleIndex < 0
                || ruleIndex >= colorRules.size()
                || other < 0
                || other >= colorRules.size()) {
            return;
        }
        Collections.swap(colorRules, ruleIndex, other);
        onChange.run();
    }

    void moveRuleToTop(int ruleIndex) {
        if (ruleIndex <= 0 || ruleIndex >= colorRules.size()) {
            return;
        }
        ColorMapping rule = colorRules.remove(ruleIndex);
        colorRules.add(0, rule);
        onChange.run();
    }

    void moveRuleToBottom(int ruleIndex) {
        int lastIndex = colorRules.size() - 1;
        if (ruleIndex < 0 || ruleIndex >= lastIndex) {
            return;
        }
        ColorMapping rule = colorRules.remove(ruleIndex);
        colorRules.add(rule);
        onChange.run();
    }

    void removeRule(int ruleIndex) {
        if (ruleIndex < 0 || ruleIndex >= colorRules.size()) {
            return;
        }
        colorRules.remove(ruleIndex);
        if (colorRules.isEmpty()) {
            colorRules.add(new ColorMapping());
        }
        onChange.run();
    }

    void removeRulesWithTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        boolean removed =
                colorRules.removeIf(rule -> rule.getTag() != null && tags.contains(rule.getTag()));
        if (!removed) {
            return;
        }
        ensureNonEmptyAndNotify();
    }

    void removeOrphanNeonRules(Predicate<String> isNeonTag, Collection<String> existingTags) {
        if (isNeonTag == null || existingTags == null) {
            return;
        }
        boolean removed =
                colorRules.removeIf(
                        rule -> {
                            String tag = rule.getTag();
                            return tag != null
                                    && isNeonTag.test(tag)
                                    && !existingTags.contains(tag);
                        });
        if (!removed) {
            return;
        }
        ensureNonEmptyAndNotify();
    }

    private void ensureNonEmptyAndNotify() {
        if (colorRules.isEmpty()) {
            colorRules.add(new ColorMapping());
        }
        onChange.run();
    }

    boolean addColorMapping(String tag, int color) {
        if (tag == null || tag.isBlank()) {
            LOGGER.debug("addColorMapping rejected: tag null or blank (color={})", color);
            return false;
        }
        if (!tagValidator.test(tag)) {
            LOGGER.debug("addColorMapping rejected: unknown tag \"{}\" (color={})", tag, color);
            return false;
        }
        Color newColor = new Color(color);
        ColorMapping newMapping = new ColorMapping(tag, newColor);
        if (colorRules.contains(newMapping)) {
            return true;
        }
        colorRules.add(newMapping);
        if (!getPalette().contains(newColor)) {
            addToPalette(newColor);
        }
        onChange.run();
        return true;
    }

    Color resolveColor(List<String> tags) {
        for (ColorMapping colorMapping : colorRules) {
            String tag = colorMapping.getTag();
            if (tag != null && tags.contains(tag)) {
                return colorMapping.getColor();
            }
        }
        return null;
    }
}
