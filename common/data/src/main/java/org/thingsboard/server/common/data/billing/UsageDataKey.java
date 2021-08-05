/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.data.billing;

import lombok.Getter;
import org.thingsboard.server.common.data.ApiUsageRecordKey;

@Getter
public enum UsageDataKey {
    DEVICES("Managed IoT Devices usage charges", 30010),
    ASSETS("Assets usage charges", 30020),
    USERS("User Accounts charges", 30030),
    DASHBOARDS("Dashboards usage charges", 30040),
    RULE_CHAINS("Rule chains usage charges", 30050),
    INTEGRATIONS("Integrations usage charges", 30060),
    CONVERTERS("Data Converters usage charges", 30070),
    SCHEDULER_EVENTS("Schedule events usage charges", 30080),

    TRANSPORT_DATA_POINTS("Data Points usage charges", 30100, ApiUsageRecordKey.TRANSPORT_DP_COUNT),
    RULE_ENGINE_EXECUTIONS("Rule engine executions charges", 30110, ApiUsageRecordKey.RE_EXEC_COUNT),
    JS_EXECUTIONS("JavaScript executions charges", 30120, ApiUsageRecordKey.JS_EXEC_COUNT),
    DATA_POINTS_STORAGE_DAYS("Maximal Storage days usage charge (TTL)", 30130, ApiUsageRecordKey.STORAGE_DP_COUNT),
    EMAILS_SENT("Email usage charges", 30150, ApiUsageRecordKey.EMAIL_EXEC_COUNT),
    SMS_SENT("SMS usage charges", 30160, ApiUsageRecordKey.SMS_EXEC_COUNT),

    WHITE_LABELING("White Label usage charges", 20010, 2, "Monthly Recurring Charges");

    private final String description;
    private final int materialNumber;
    private ApiUsageRecordKey apiUsageRecordKey;
    private String invoiceType = "Usage Charges";
    private int chargedUnit = 1;
    private int subdocId = 9;

    UsageDataKey(String description, int materialNumber) {
        this.description = description;
        this.materialNumber = materialNumber;
    }

    UsageDataKey(String description, int materialNumber, ApiUsageRecordKey apiUsageRecordKey) {
        this.description = description;
        this.materialNumber = materialNumber;
        this.apiUsageRecordKey = apiUsageRecordKey;
    }

    UsageDataKey(String description, int materialNumber, String invoiceType) {
        this.description = description;
        this.materialNumber = materialNumber;
        this.invoiceType = invoiceType;
    }

    UsageDataKey(String description, int materialNumber, int subdocId, String invoiceType) {
        this.description = description;
        this.materialNumber = materialNumber;
        this.subdocId = subdocId;
        this.invoiceType = invoiceType;
    }

    UsageDataKey(String description, int materialNumber, ApiUsageRecordKey apiUsageRecordKey, String invoiceType, int chargedUnit) {
        this.description = description;
        this.materialNumber = materialNumber;
        this.apiUsageRecordKey = apiUsageRecordKey;
        this.invoiceType = invoiceType;
        this.chargedUnit = chargedUnit;
    }

}
