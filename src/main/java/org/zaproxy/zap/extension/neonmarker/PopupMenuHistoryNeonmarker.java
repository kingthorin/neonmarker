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

import org.parosproxy.paros.Constant;
import org.zaproxy.zap.view.messagecontainer.http.HttpMessageContainer;
import org.zaproxy.zap.view.popup.PopupMenuHistoryReferenceContainer;

public class PopupMenuHistoryNeonmarker extends PopupMenuHistoryReferenceContainer {

    private static final long serialVersionUID = 5126729231139965308L;

    public PopupMenuHistoryNeonmarker() {
        super(Constant.messages.getString("neonmarker.popup.label"), true);
        setIcon(ExtensionNeonmarker.getIcon());
        setButtonStateOverriddenByChildren(false);
        PopupMenuItemHistoryColor setColor =
                new PopupMenuItemHistoryColor(
                        Constant.messages.getString("neonmarker.popup.item.color.label"));
        PopupMenuItemHistoryUntag unTag =
                new PopupMenuItemHistoryUntag(
                        Constant.messages.getString("neonmarker.popup.item.untag.label"));
        add(setColor);
        add(unTag);
    }

    @Override
    public boolean isEnableForInvoker(Invoker invoker, HttpMessageContainer httpMessageContainer) {
        return (invoker == Invoker.HISTORY_PANEL);
    }

    @Override
    public boolean isSafe() {
        return true;
    }

    @Override
    public boolean precedeWithSeparator() {
        return true;
    }
}
