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
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@Cacheable("false")
@PersistenceCapable(table = "NEONMARKER_TAG_RULE", detachable = "true")
class TagRuleEntry {

    @SuppressWarnings("unused")
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    @Column(name = "ID")
    private long id;

    @SuppressWarnings("unused")
    @Column(name = "SORT_ORDER")
    private int sortOrder;

    @Column(name = "TAG", length = 4096)
    private String tag;

    @Column(name = "COLOR")
    private int color;

    @Column(name = "ENABLED")
    private boolean enabled;

    TagRuleEntry() {}

    TagRuleEntry(int sortOrder, String tag, int color, boolean enabled) {
        this.sortOrder = sortOrder;
        this.tag = tag;
        this.color = color;
        this.enabled = enabled;
    }

    String getTag() {
        return tag;
    }

    int getColor() {
        return color;
    }

    boolean isEnabled() {
        return enabled;
    }
}
