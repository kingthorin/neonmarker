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

import javax.jdo.annotations.Cacheable;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

@Cacheable("false")
@PersistenceCapable(table = "NEONMARKER_HISTORY_COLOR", detachable = "true")
class HistoryColorEntry {

    @PrimaryKey
    @Column(name = "HISTORY_ID")
    private int historyId;

    @Column(name = "COLOR")
    private int color;

    HistoryColorEntry() {}

    HistoryColorEntry(int historyId, int color) {
        this.historyId = historyId;
        this.color = color;
    }

    int getHistoryId() {
        return historyId;
    }

    int getColor() {
        return color;
    }

    void setColor(int color) {
        this.color = color;
    }
}
