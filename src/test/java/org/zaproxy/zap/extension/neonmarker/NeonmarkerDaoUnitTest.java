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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.db.Database;
import org.parosproxy.paros.db.DatabaseServer;

/** Round-trips {@link NeonmarkerDao} through a real in-memory HSQLDB schema. */
class NeonmarkerDaoUnitTest {

    private NeonmarkerDao dao;

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:hsqldb:mem:neonmarker-" + UUID.randomUUID();
        DatabaseServer server =
                new DatabaseServer() {
                    @Override
                    public String getUrl() {
                        return url;
                    }

                    @Override
                    public String getUser() {
                        return "SA";
                    }

                    @Override
                    public String getPassword() {
                        return "";
                    }
                };
        Database db =
                (Database)
                        Proxy.newProxyInstance(
                                Database.class.getClassLoader(),
                                new Class<?>[] {Database.class},
                                (proxy, method, args) ->
                                        "getDatabaseServer".equals(method.getName())
                                                ? server
                                                : null);
        new TableJdo(db);
        dao = new NeonmarkerDao();
    }

    @AfterEach
    void tearDown() {
        TableJdo.closePmf();
    }

    @Test
    void shouldRoundTripRulesInOrderSkippingPlaceholders() {
        // Given
        ColorMapping disabled = new ColorMapping("B", Color.BLUE);
        disabled.setEnabled(false);
        // When
        dao.saveRules(List.of(new ColorMapping("A", Color.RED), new ColorMapping(), disabled));
        List<ColorMapping> rules = dao.loadRules();
        // Then
        assertEquals(2, rules.size());
        assertEquals("A", rules.get(0).getTag());
        assertEquals(Color.RED, rules.get(0).getColor());
        assertTrue(rules.get(0).isEnabled());
        assertEquals("B", rules.get(1).getTag());
        assertFalse(rules.get(1).isEnabled());
    }

    @Test
    void shouldReplaceRulesOnSave() {
        // Given
        dao.saveRules(List.of(new ColorMapping("A", Color.RED)));
        // When
        dao.saveRules(List.of(new ColorMapping("C", Color.GREEN)));
        // Then
        List<ColorMapping> rules = dao.loadRules();
        assertEquals(1, rules.size());
        assertEquals("C", rules.get(0).getTag());
    }

    @Test
    void shouldInsertUpdateAndDeleteHistoryColors() {
        // Given
        dao.putHistoryColors(List.of(1, 2), Color.RED);
        // When
        dao.putHistoryColors(List.of(2), Color.BLUE);
        dao.deleteHistoryColors(List.of(1, 99));
        // Then
        assertEquals(Map.of(2, Color.BLUE), dao.loadHistoryColors());
    }

    @Test
    void shouldInsertAndUpdateSettings() {
        // Given
        dao.saveSetting(NeonmarkerColorService.SETTING_TAG_LAYER, "true");
        // When
        dao.saveSetting(NeonmarkerColorService.SETTING_TAG_LAYER, "false");
        dao.saveSetting(NeonmarkerColorService.SETTING_ARBITRARY_LAYER, "true");
        // Then
        assertEquals(
                Map.of(
                        NeonmarkerColorService.SETTING_TAG_LAYER, "false",
                        NeonmarkerColorService.SETTING_ARBITRARY_LAYER, "true"),
                dao.loadSettings());
    }
}
