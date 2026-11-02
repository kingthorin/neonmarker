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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit test for {@link NeonmarkerColorService}. */
class NeonmarkerColorServiceUnitTest {

    private NeonmarkerColorService service;

    @BeforeEach
    void setUp() {
        service = new NeonmarkerColorService(tag -> true, () -> {});
    }

    @Test
    void shouldReturnNullWhenNoRulesMatch() {
        // Given / When
        Color colour = service.resolveColor(1, List.of("unknown"));
        // Then
        assertNull(colour);
    }

    @Test
    void shouldReturnColorForMatchingTag() {
        // Given
        service.addColorMapping("Comment", 0xff0000);
        // When
        Color colour = service.resolveColor(1, List.of("Comment"));
        // Then
        assertEquals(new Color(0xff0000), colour);
    }

    @Test
    void shouldUseFirstMatchingRuleInListOrder() {
        // Given
        service.addColorMapping("B", Color.BLUE.getRGB());
        service.addColorMapping("A", Color.RED.getRGB());
        // When
        Color colour = service.resolveColor(1, List.of("A", "B"));
        // Then
        assertEquals(Color.BLUE, colour);
    }

    @Test
    void shouldRejectInvalidTag() {
        // Given
        NeonmarkerColorService strict = new NeonmarkerColorService(tag -> false, () -> {});
        // When
        boolean added = strict.addColorMapping("Comment", 0xff0000);
        // Then
        assertFalse(added);
        assertTrue(strict.getColorRules().isEmpty());
    }

    @Test
    void shouldNotDuplicateIdenticalMapping() {
        // Given
        assertTrue(service.addColorMapping("Comment", 0xff0000));
        // When
        assertTrue(service.addColorMapping("Comment", 0xff0000));
        // Then
        assertEquals(1, service.getColorRules().size());
    }

    @Test
    void shouldSwapRulesAndChangePrecedence() {
        // Given
        service.addColorMapping("A", Color.RED.getRGB());
        service.addColorMapping("B", Color.BLUE.getRGB());
        // When
        service.swapRules(1, true);
        Color colour = service.resolveColor(1, List.of("A", "B"));
        // Then
        assertEquals(Color.BLUE, colour);
    }

    @Test
    void shouldIgnoreInvalidSwapIndexes() {
        // Given
        service.addColorMapping("A", Color.RED.getRGB());
        service.addColorMapping("B", Color.BLUE.getRGB());
        // When
        service.swapRules(0, true);
        service.swapRules(1, false);
        service.swapRules(-1, false);
        service.swapRules(2, true);
        // Then
        assertEquals(Color.RED, service.resolveColor(1, List.of("A", "B")));
    }

    @Test
    void shouldMoveRuleToTop() {
        // Given
        service.addColorMapping("A", Color.RED.getRGB());
        service.addColorMapping("B", Color.BLUE.getRGB());
        service.addColorMapping("C", Color.GREEN.getRGB());
        // When
        service.moveRuleToTop(2);
        Color colour = service.resolveColor(1, List.of("A", "B", "C"));
        // Then
        assertEquals(Color.GREEN, colour);
    }

    @Test
    void shouldMoveRuleToBottom() {
        // Given
        service.addColorMapping("A", Color.RED.getRGB());
        service.addColorMapping("B", Color.BLUE.getRGB());
        // When
        service.moveRuleToBottom(0);
        Color colour = service.resolveColor(1, List.of("A", "B"));
        // Then
        assertEquals(Color.BLUE, colour);
    }

    @Test
    void shouldRemoveRuleAndLeaveOneEmptyRuleWhenLastRemoved() {
        // Given
        service.addColorMapping("Comment", 0xff0000);
        // When
        service.removeRule(0);
        // Then
        assertEquals(1, service.getColorRules().size());
        assertNull(service.getColorRules().get(0).getTag());
    }

    @Test
    void shouldIgnoreInvalidRemoveIndex() {
        // Given
        service.addColorMapping("Comment", 0xff0000);
        // When
        service.removeRule(-1);
        service.removeRule(1);
        // Then
        assertEquals(1, service.getColorRules().size());
        assertEquals("Comment", service.getColorRules().get(0).getTag());
    }

    @Test
    void shouldRemoveRuleWithoutAddingEmptyRuleWhenOthersRemain() {
        // Given
        service.addColorMapping("A", Color.RED.getRGB());
        service.addColorMapping("B", Color.BLUE.getRGB());
        // When
        service.removeRule(0);
        // Then
        assertEquals(1, service.getColorRules().size());
        assertEquals("B", service.getColorRules().get(0).getTag());
    }

    @Test
    void shouldIgnoreRulesWithNullTag() {
        // Given
        service.addEmptyRule();
        service.addColorMapping("Comment", 0xff0000);
        // When
        Color colour = service.resolveColor(1, List.of("Comment"));
        // Then
        assertEquals(new Color(0xff0000), colour);
    }

    @Test
    void shouldInvokeOnChangeWhenRulesMutate() {
        // Given
        AtomicInteger changes = new AtomicInteger();
        NeonmarkerColorService notifying =
                new NeonmarkerColorService(tag -> true, changes::incrementAndGet);
        // When
        notifying.addEmptyRule();
        notifying.addColorMapping("A", Color.RED.getRGB());
        notifying.swapRules(1, true);
        notifying.moveRuleToTop(1);
        notifying.moveRuleToBottom(0);
        notifying.removeRule(0);
        notifying.clearRules();
        // Then
        assertEquals(7, changes.get());
    }

    @Test
    void shouldLeaveOneEmptyRuleWhenCleared() {
        // Given
        service.addColorMapping("Comment", 0xff0000);
        // When
        service.clearRules();
        // Then
        assertEquals(1, service.getColorRules().size());
        assertNull(service.getColorRules().get(0).getTag());
    }

    @Test
    void shouldRemoveRulesMatchingTagsAndLeaveEmptyRuleWhenNoneRemain() {
        // Given
        service.addColorMapping("neon_a", Color.RED.getRGB());
        service.addColorMapping("Comment", Color.BLUE.getRGB());
        // When
        service.removeRulesWithTags(List.of("neon_a"));
        // Then
        assertEquals(1, service.getColorRules().size());
        assertEquals("Comment", service.getColorRules().get(0).getTag());

        // When
        service.removeRulesWithTags(List.of("Comment"));
        // Then
        assertEquals(1, service.getColorRules().size());
        assertNull(service.getColorRules().get(0).getTag());
    }

    @Test
    void shouldRemoveOrphanNeonRulesButKeepNonNeonAndPresentNeonTags() {
        // Given
        service.addColorMapping("neon_gone", Color.RED.getRGB());
        service.addColorMapping("neon_kept", Color.GREEN.getRGB());
        service.addColorMapping("Comment", Color.BLUE.getRGB());
        // When
        service.removeOrphanNeonRules(
                tag -> tag.startsWith("neon_"), List.of("neon_kept", "Comment"));
        // Then
        assertEquals(2, service.getColorRules().size());
        assertEquals("neon_kept", service.getColorRules().get(0).getTag());
        assertEquals("Comment", service.getColorRules().get(1).getTag());
    }

    @Test
    void shouldLeaveEmptyRuleWhenAllOrphanNeonRulesRemoved() {
        // Given
        service.addColorMapping("neon_dead", Color.RED.getRGB());
        // When
        service.removeOrphanNeonRules(tag -> tag.startsWith("neon_"), List.of());
        // Then
        assertEquals(1, service.getColorRules().size());
        assertNull(service.getColorRules().get(0).getTag());
    }

    @Test
    void shouldReplaceHistoryColorOnSameId() {
        // Given
        service.setHistoryColors(List.of(10), Color.RED);
        // When
        service.setHistoryColors(List.of(10), Color.BLUE);
        // Then
        assertEquals(Color.BLUE, service.resolveColor(10, List.of()));
    }

    @Test
    void shouldSetHistoryColorsOnMultipleIds() {
        // Given / When
        service.setHistoryColors(List.of(1, 2, 3), Color.GREEN);
        // Then
        assertEquals(Color.GREEN, service.resolveColor(1, List.of()));
        assertEquals(Color.GREEN, service.resolveColor(2, List.of()));
        assertEquals(Color.GREEN, service.resolveColor(3, List.of()));
    }

    @Test
    void shouldPreferArbitraryColorOverTagRule() {
        // Given
        service.addColorMapping("Comment", Color.RED.getRGB());
        service.setHistoryColors(List.of(5), Color.BLUE);
        // When
        Color colour = service.resolveColor(5, List.of("Comment"));
        // Then
        assertEquals(Color.BLUE, colour);
    }

    @Test
    void shouldSkipArbitraryLayerWhenDisabled() {
        // Given
        service.addColorMapping("Comment", Color.RED.getRGB());
        service.setHistoryColors(List.of(5), Color.BLUE);
        service.setArbitraryLayerEnabled(false);
        // When
        Color colour = service.resolveColor(5, List.of("Comment"));
        // Then
        assertEquals(Color.RED, colour);
    }

    @Test
    void shouldSkipTagLayerWhenDisabled() {
        // Given
        service.addColorMapping("Comment", Color.RED.getRGB());
        service.setTagLayerEnabled(false);
        // When
        Color colour = service.resolveColor(1, List.of("Comment"));
        // Then
        assertNull(colour);
    }

    @Test
    void shouldSkipDisabledTagRule() {
        // Given
        service.addColorMapping("A", Color.RED.getRGB());
        service.addColorMapping("B", Color.BLUE.getRGB());
        service.getColorRules().get(0).setEnabled(false);
        // When
        Color colour = service.resolveColor(1, List.of("A", "B"));
        // Then
        assertEquals(Color.BLUE, colour);
    }

    @Test
    void shouldFallBackToTagAfterClearingHistoryColor() {
        // Given
        service.addColorMapping("Comment", Color.RED.getRGB());
        service.setHistoryColors(List.of(5), Color.BLUE);
        // When
        service.clearHistoryColors(List.of(5));
        // Then
        assertEquals(Color.RED, service.resolveColor(5, List.of("Comment")));
    }

    @Test
    void shouldDropHistoryColorWithoutNotifying() {
        // Given
        AtomicInteger changes = new AtomicInteger();
        NeonmarkerColorService notifying =
                new NeonmarkerColorService(tag -> true, changes::incrementAndGet);
        notifying.setHistoryColors(List.of(7), Color.RED);
        changes.set(0);
        // When
        notifying.dropHistoryColor(7);
        // Then
        assertNull(notifying.resolveColor(7, List.of()));
        assertEquals(0, changes.get());
    }

    @Test
    void shouldNotNotifyWhenSettingHistoryColorsWithNullColor() {
        // Given
        AtomicInteger changes = new AtomicInteger();
        NeonmarkerColorService notifying =
                new NeonmarkerColorService(tag -> true, changes::incrementAndGet);
        // When
        notifying.setHistoryColors(List.of(1), null);
        // Then
        assertEquals(0, changes.get());
        assertNull(notifying.resolveColor(1, List.of()));
    }

    @Test
    void shouldNotNotifyWhenSettingHistoryColorsWithEmptyIds() {
        // Given
        AtomicInteger changes = new AtomicInteger();
        NeonmarkerColorService notifying =
                new NeonmarkerColorService(tag -> true, changes::incrementAndGet);
        // When
        notifying.setHistoryColors(List.of(), Color.RED);
        // Then
        assertEquals(0, changes.get());
    }

    @Test
    void shouldNotNotifyWhenClearingHistoryColorsWithEmptyIds() {
        // Given
        AtomicInteger changes = new AtomicInteger();
        NeonmarkerColorService notifying =
                new NeonmarkerColorService(tag -> true, changes::incrementAndGet);
        notifying.setHistoryColors(List.of(1), Color.RED);
        changes.set(0);
        // When
        notifying.clearHistoryColors(List.of());
        // Then
        assertEquals(0, changes.get());
        assertEquals(Color.RED, notifying.resolveColor(1, List.of()));
    }

    @Test
    void shouldPersistRulesOnEveryRuleChange() {
        // Given
        RecordingStore store = new RecordingStore();
        service.setStore(store);
        // When
        service.addColorMapping("A", Color.RED.getRGB());
        service.addColorMapping("B", Color.BLUE.getRGB());
        service.swapRules(1, true);
        // Then
        assertEquals(3, store.ruleSaves);
        assertEquals("B", store.savedRules.get(0).getTag());
    }

    @Test
    void shouldPersistRuleEnabledColorAndTagEdits() {
        // Given
        RecordingStore store = new RecordingStore();
        service.addColorMapping("A", Color.RED.getRGB());
        service.setStore(store);
        ColorMapping rule = service.getColorRules().get(0);
        // When
        service.setRuleEnabled(rule, false);
        service.setRuleColor(rule, Color.GREEN);
        service.setRuleTag(rule, "B");
        // Then
        assertEquals(3, store.ruleSaves);
        assertFalse(store.savedRules.get(0).isEnabled());
        assertEquals(Color.GREEN, store.savedRules.get(0).getColor());
        assertEquals("B", store.savedRules.get(0).getTag());
    }

    @Test
    void shouldPersistHistoryColorSetClearAndDrop() {
        // Given
        RecordingStore store = new RecordingStore();
        service.setStore(store);
        // When
        service.setHistoryColors(List.of(1, 2), Color.RED);
        service.clearHistoryColors(List.of(1));
        service.dropHistoryColor(2);
        // Then
        assertEquals(Map.of(), store.historyColors);
        assertEquals(2, store.historyDeletes);
    }

    @Test
    void shouldNotPersistDropOfUnknownHistoryColor() {
        // Given
        RecordingStore store = new RecordingStore();
        service.setStore(store);
        // When
        service.dropHistoryColor(42);
        // Then
        assertEquals(0, store.historyDeletes);
    }

    @Test
    void shouldPersistLayerSettings() {
        // Given
        RecordingStore store = new RecordingStore();
        service.setStore(store);
        // When
        service.setArbitraryLayerEnabled(false);
        service.setTagLayerEnabled(false);
        // Then
        assertEquals("false", store.settings.get(NeonmarkerColorService.SETTING_ARBITRARY_LAYER));
        assertEquals("false", store.settings.get(NeonmarkerColorService.SETTING_TAG_LAYER));
    }

    @Test
    void shouldReloadStateFromStore() {
        // Given
        RecordingStore store = new RecordingStore();
        store.savedRules = List.of(new ColorMapping("Comment", Color.RED));
        store.historyColors.put(5, Color.BLUE);
        store.settings.put(NeonmarkerColorService.SETTING_TAG_LAYER, "false");
        service.setHistoryColors(List.of(9), Color.GREEN);
        service.setStore(store);
        // When
        service.reload();
        // Then
        assertEquals(1, service.getColorRules().size());
        assertEquals("Comment", service.getColorRules().get(0).getTag());
        assertEquals(Color.BLUE, service.resolveColor(5, List.of()));
        assertNull(service.resolveColor(9, List.of()));
        assertTrue(service.isArbitraryLayerEnabled());
        assertFalse(service.isTagLayerEnabled());
        assertEquals(0, store.ruleSaves);
    }

    @Test
    void shouldReloadWithPlaceholderRuleWhenStoreEmpty() {
        // Given
        service.addColorMapping("A", Color.RED.getRGB());
        service.setStore(new RecordingStore());
        // When
        service.reload();
        // Then
        assertEquals(1, service.getColorRules().size());
        assertNull(service.getColorRules().get(0).getTag());
    }

    private static class RecordingStore implements NeonmarkerStore {
        List<ColorMapping> savedRules = List.of();
        int ruleSaves;
        final Map<Integer, Color> historyColors = new HashMap<>();
        int historyDeletes;
        final Map<String, String> settings = new HashMap<>();

        @Override
        public List<ColorMapping> loadRules() {
            return savedRules;
        }

        @Override
        public Map<Integer, Color> loadHistoryColors() {
            return historyColors;
        }

        @Override
        public Map<String, String> loadSettings() {
            return settings;
        }

        @Override
        public void saveRules(List<ColorMapping> rules) {
            savedRules = List.copyOf(rules);
            ruleSaves++;
        }

        @Override
        public void putHistoryColors(Collection<Integer> historyIds, Color color) {
            historyIds.forEach(id -> historyColors.put(id, color));
        }

        @Override
        public void deleteHistoryColors(Collection<Integer> historyIds) {
            historyIds.forEach(historyColors::remove);
            historyDeletes++;
        }

        @Override
        public void saveSetting(String key, String value) {
            settings.put(key, value);
        }
    }

    @Test
    void shouldNotNotifyWhenClearingHistoryColorsWithNoMatch() {
        // Given
        AtomicInteger changes = new AtomicInteger();
        NeonmarkerColorService notifying =
                new NeonmarkerColorService(tag -> true, changes::incrementAndGet);
        notifying.setHistoryColors(List.of(1), Color.RED);
        changes.set(0);
        // When
        notifying.clearHistoryColors(List.of(99));
        // Then
        assertEquals(0, changes.get());
        assertEquals(Color.RED, notifying.resolveColor(1, List.of()));
    }
}
