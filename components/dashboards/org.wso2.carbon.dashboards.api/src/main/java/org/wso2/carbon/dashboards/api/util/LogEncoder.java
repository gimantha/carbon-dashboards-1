/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.dashboards.api.util;

import org.owasp.encoder.Encode;

/**
 * Class used to encode strings before logging.
 */
public class LogEncoder {

    private LogEncoder() {}

    public static String getEncodedString(String str) {
        String cleanedString = str.replace('\n', '_').replace('\r', '_');
        cleanedString = Encode.forHtml(cleanedString);
        if (!cleanedString.equals(str)) {
            cleanedString += " (Encoded)";
        }
        return cleanedString;
    }
}
