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
import java.util.List;
import javax.swing.JColorChooser;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.model.HistoryReference;
import org.zaproxy.zap.view.popup.PopupMenuItemHistoryReferenceContainer;

@SuppressWarnings("serial")
public class PopupMenuItemHistoryColor extends PopupMenuItemHistoryReferenceContainer {

    private static final long serialVersionUID = 2746419567363361343L;

    private final boolean clear;

    public PopupMenuItemHistoryColor(String label) {
        this(label, false);
    }

    public PopupMenuItemHistoryColor(String label, boolean clear) {
        super(label, true);
        this.clear = clear;
    }

    @Override
    public void performHistoryReferenceActions(List<HistoryReference> hrefs) {
        NeonmarkerColorService colorService =
                ExtensionNeonmarker.getExtension(ExtensionNeonmarker.class).getColorService();
        List<Integer> ids = hrefs.stream().map(HistoryReference::getHistoryId).toList();
        if (clear) {
            colorService.clearHistoryColors(ids);
            return;
        }
        Color newColor =
                JColorChooser.showDialog(
                        this,
                        Constant.messages.getString("neonmarker.panel.color.chooser.title"),
                        Color.WHITE);
        if (newColor == null) {
            return;
        }
        colorService.setHistoryColors(ids, newColor);
    }

    @Override
    protected void performAction(HistoryReference historyReference) {
        // Nothing to do
    }
}
