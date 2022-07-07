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

import java.util.List;
import java.util.stream.Collectors;
import org.parosproxy.paros.model.HistoryReference;
import org.zaproxy.zap.view.popup.PopupMenuItemHistoryReferenceContainer;

public class PopupMenuItemHistoryUntag extends PopupMenuItemHistoryReferenceContainer {

    private static final long serialVersionUID = 2746419567363361343L;

    public PopupMenuItemHistoryUntag(String label) {
        super(label, true);
    }

    @Override
    public void performHistoryReferenceActions(List<HistoryReference> hrefs) {
        hrefs.forEach(
                hr -> {
                    List<String> tagsBeingKept =
                            hr.getTags().stream()
                                    .filter(
                                            tag ->
                                                    !ExtensionNeonmarker.TAG_PATTERN
                                                            .matcher(tag)
                                                            .matches())
                                    .collect(Collectors.toList());
                    hr.setTags(tagsBeingKept);
                });
    }

    @Override
    protected void performAction(HistoryReference historyReference) {
        // Nothing to do.
    }
}
