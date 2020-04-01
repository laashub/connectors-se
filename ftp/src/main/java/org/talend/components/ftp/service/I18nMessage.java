/*
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.components.ftp.service;

import org.talend.sdk.component.api.internationalization.Internationalized;

@Internationalized
public interface I18nMessage {

    String hostRequired();

    String successConnection();

    String warnCannotDisconnect(String message);

    String statusNotConnected();

    String errorConnection(String message);

    String errorListFiles(String message);

    String errorInitWriter(String message);

    String errorCreateRemoteFile(String path, String replyCode);

    String infoCreateRemoteFile(String path);

    String errorTooManyRetries(int nbMaxRetriesPut);
}
