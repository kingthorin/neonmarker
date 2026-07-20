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
import java.util.List;
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
        Color colour = service.resolveColor(List.of("unknown"));
        // Then
        assertNull(colour);
    }

    @Test
    void shouldReturnColorForMatchingTag() {
        // Given
        service.addColorMapping("Comment", 0xff0000);
        // When
        Color colour = service.resolveColor(List.of("Comment"));
        // Then
        assertEquals(new Color(0xff0000), colour);
    }

    @Test
    void shouldUseFirstMatchingRuleInListOrder() {
        // Given
        service.addColorMapping("B", Color.BLUE.getRGB());
        service.addColorMapping("A", Color.RED.getRGB());
        // When
        Color colour = service.resolveColor(List.of("A", "B"));
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
        Color colour = service.resolveColor(List.of("A", "B"));
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
        assertEquals(Color.RED, service.resolveColor(List.of("A", "B")));
    }

    @Test
    void shouldMoveRuleToTop() {
        // Given
        service.addColorMapping("A", Color.RED.getRGB());
        service.addColorMapping("B", Color.BLUE.getRGB());
        service.addColorMapping("C", Color.GREEN.getRGB());
        // When
        service.moveRuleToTop(2);
        Color colour = service.resolveColor(List.of("A", "B", "C"));
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
        Color colour = service.resolveColor(List.of("A", "B"));
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
        Color colour = service.resolveColor(List.of("Comment"));
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
}
