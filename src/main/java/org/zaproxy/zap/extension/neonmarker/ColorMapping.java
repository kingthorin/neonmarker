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
import java.util.Objects;

class ColorMapping {
    private String tag;
    private Color color;

    ColorMapping() {
        this.color = NeonmarkerColorService.getPalette().get(0);
    }

    ColorMapping(String tag, Color color) {
        this.tag = tag;
        this.color = color;
    }

    String getTag() {
        return tag;
    }

    void setTag(String tag) {
        this.tag = tag;
    }

    Color getColor() {
        return color;
    }

    void setColor(Color color) {
        this.color = color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, tag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ColorMapping)) {
            return false;
        }
        ColorMapping other = (ColorMapping) obj;
        return Objects.equals(color, other.color) && Objects.equals(tag, other.tag);
    }
}
