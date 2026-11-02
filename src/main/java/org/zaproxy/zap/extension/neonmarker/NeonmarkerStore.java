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
import java.util.Collection;
import java.util.List;
import java.util.Map;

interface NeonmarkerStore {

    NeonmarkerStore NONE = new NeonmarkerStore() {};

    default List<ColorMapping> loadRules() {
        return List.of();
    }

    default Map<Integer, Color> loadHistoryColors() {
        return Map.of();
    }

    default Map<String, String> loadSettings() {
        return Map.of();
    }

    default void saveRules(List<ColorMapping> rules) {}

    default void putHistoryColors(Collection<Integer> historyIds, Color color) {}

    default void deleteHistoryColors(Collection<Integer> historyIds) {}

    default void saveSetting(String key, String value) {}
}
