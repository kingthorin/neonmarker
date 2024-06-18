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
import java.util.UUID;
import javax.swing.JColorChooser;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.model.HistoryReference;
import org.zaproxy.zap.view.popup.PopupMenuItemHistoryReferenceContainer;

@SuppressWarnings("serial")
public class PopupMenuItemHistoryColor extends PopupMenuItemHistoryReferenceContainer {

    private static final long serialVersionUID = 2746419567363361343L;

    private ExtensionNeonmarker extNeon =
            Control.getSingleton().getExtensionLoader().getExtension(ExtensionNeonmarker.class);

    public PopupMenuItemHistoryColor(String label) {
        super(label, true);
    }

    @Override
    public void performHistoryReferenceActions(List<HistoryReference> hrefs) {
        Color newColor =
                JColorChooser.showDialog(
                        this,
                        Constant.messages.getString("neonmarker.panel.color.chooser.title"),
                        Color.WHITE);
        String uuid = ExtensionNeonmarker.TAG_PREFIX + UUID.randomUUID().toString();
        hrefs.forEach(hr -> hr.addTag(uuid));
        extNeon.addColorMapping(uuid, newColor.getRGB());
    }

    @Override
    protected void performAction(HistoryReference historyReference) {
        // Nothing to do
    }
}
