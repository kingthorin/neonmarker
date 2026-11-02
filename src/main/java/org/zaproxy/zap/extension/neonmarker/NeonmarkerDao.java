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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class NeonmarkerDao implements NeonmarkerStore {

    private static final Logger LOGGER = LogManager.getLogger(NeonmarkerDao.class);

    @Override
    public List<ColorMapping> loadRules() {
        return read(
                pm -> {
                    Query<TagRuleEntry> query = pm.newQuery(TagRuleEntry.class);
                    query.setOrdering("sortOrder ASC");
                    List<ColorMapping> rules = new ArrayList<>();
                    for (TagRuleEntry entry : query.executeList()) {
                        ColorMapping rule =
                                new ColorMapping(entry.getTag(), new Color(entry.getColor(), true));
                        rule.setEnabled(entry.isEnabled());
                        rules.add(rule);
                    }
                    return rules;
                },
                List.of());
    }

    @Override
    public Map<Integer, Color> loadHistoryColors() {
        return read(
                pm -> {
                    Map<Integer, Color> colors = new HashMap<>();
                    for (HistoryColorEntry entry :
                            pm.newQuery(HistoryColorEntry.class).executeList()) {
                        colors.put(entry.getHistoryId(), new Color(entry.getColor(), true));
                    }
                    return colors;
                },
                Map.of());
    }

    @Override
    public Map<String, String> loadSettings() {
        return read(
                pm -> {
                    Map<String, String> settings = new HashMap<>();
                    for (SettingEntry entry : pm.newQuery(SettingEntry.class).executeList()) {
                        settings.put(entry.getKey(), entry.getValue());
                    }
                    return settings;
                },
                Map.of());
    }

    @Override
    public void saveRules(List<ColorMapping> rules) {
        write(
                pm -> {
                    pm.newQuery(TagRuleEntry.class).deletePersistentAll();
                    int order = 0;
                    for (ColorMapping rule : rules) {
                        if (rule.getTag() == null) {
                            continue;
                        }
                        pm.makePersistent(
                                new TagRuleEntry(
                                        order++,
                                        rule.getTag(),
                                        rule.getColor().getRGB(),
                                        rule.isEnabled()));
                    }
                });
    }

    @Override
    public void putHistoryColors(Collection<Integer> historyIds, Color color) {
        write(
                pm -> {
                    for (Integer historyId : historyIds) {
                        try {
                            pm.getObjectById(HistoryColorEntry.class, historyId)
                                    .setColor(color.getRGB());
                        } catch (JDOObjectNotFoundException e) {
                            pm.makePersistent(new HistoryColorEntry(historyId, color.getRGB()));
                        }
                    }
                });
    }

    @Override
    public void deleteHistoryColors(Collection<Integer> historyIds) {
        write(
                pm -> {
                    for (Integer historyId : historyIds) {
                        try {
                            pm.deletePersistent(
                                    pm.getObjectById(HistoryColorEntry.class, historyId));
                        } catch (JDOObjectNotFoundException e) {
                            // Already gone.
                        }
                    }
                });
    }

    @Override
    public void saveSetting(String key, String value) {
        write(
                pm -> {
                    try {
                        pm.getObjectById(SettingEntry.class, key).setValue(value);
                    } catch (JDOObjectNotFoundException e) {
                        pm.makePersistent(new SettingEntry(key, value));
                    }
                });
    }

    private static <T> T read(Function<PersistenceManager, T> action, T fallback) {
        PersistenceManagerFactory pmf = TableJdo.getPmf();
        if (pmf == null) {
            return fallback;
        }
        PersistenceManager pm = pmf.getPersistenceManager();
        try {
            return action.apply(pm);
        } catch (Exception e) {
            LOGGER.error("Failed to load Neonmarker data:", e);
            return fallback;
        } finally {
            pm.close();
        }
    }

    private static void write(Consumer<PersistenceManager> action) {
        PersistenceManagerFactory pmf = TableJdo.getPmf();
        if (pmf == null) {
            return;
        }
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            action.accept(pm);
            tx.commit();
        } catch (Exception e) {
            LOGGER.warn("Failed to persist Neonmarker data:", e);
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }
}
