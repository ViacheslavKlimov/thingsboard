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
package org.thingsboard.server.common.data;

import lombok.Getter;

public enum ApiUsageRecordKey {

    TRANSPORT_MSG_COUNT(ApiFeature.TRANSPORT, "transportMsgCount", "transportMsgLimit"),
    TRANSPORT_DP_COUNT(ApiFeature.TRANSPORT, "transportDataPointsCount", "transportDataPointsLimit"),
    STORAGE_DP_COUNT(ApiFeature.DB, "storageDataPointsCount", "storageDataPointsLimit"),
    RE_EXEC_COUNT(ApiFeature.RE, "ruleEngineExecutionCount", "ruleEngineExecutionLimit"),
    FAILED_RE_EXEC_COUNT("failedRuleEngineExecutionCount"),
    JS_EXEC_COUNT(ApiFeature.JS, "jsExecutionCount", "jsExecutionLimit"),
    EMAIL_EXEC_COUNT(ApiFeature.EMAIL, "emailCount", "emailLimit"),
    SMS_EXEC_COUNT(ApiFeature.SMS, "smsCount", "smsLimit"),
    CREATED_ALARMS_COUNT(ApiFeature.ALARM, "createdAlarmsCount", "createdAlarmsLimit"),

    REST_API_CALLS_COUNT("restApiCallsCount"),
    FAILED_REST_API_CALLS_COUNT("failedRestApiCallsCount"),

    UPLINK_MSG_COUNT("uplinkMsgCount"),
    FAILED_UPLINK_MSG_COUNT("failedUplinkMsgCount"),
    DOWNLINK_MSG_COUNT("downlinkMsgCount"),
    FAILED_DOWNLINK_MSG_COUNT("failedDownlinkMsgCount"),

    ONE_WAY_RPC_REQUEST_COUNT("oneWayRpcCount"),
    FAILED_ONE_WAY_RPC_REQUEST_COUNT("failedOneWayRpcCount"),
    TWO_WAY_RPC_REQUEST_COUNT("twoWayRpcCount"),
    FAILED_TWO_WAY_RPC_REQUEST_COUNT("failedTwoWayRpcCount"),

    QUEUED_PERSISTENT_RPC_REQUEST_COUNT("queuedPersistentRpcRequestCount"),
    DELIVERED_PERSISTENT_RPC_REQUEST_COUNT("deliveredPersistentRpcRequestCount"),
    SUCCESSFUL_PERSISTENT_RPC_REQUEST_COUNT("successfulPersistentRpcRequestCount"),
    TIMED_OUT_PERSISTENT_RPC_REQUEST_COUNT("timedOutPersistentRpcRequestCount"),
    FAILED_PERSISTENT_RPC_REQUEST_COUNT("failedPersistentRpcRequestCount"),

    TMA_VPN_DATA_IN("tmaVpnDataIn"),
    TMA_VPN_DATA_OUT("tmaVpnDataOut"),
    WBC_VPN_DATA_IN("wbcVpnDataIn"),
    WBC_VPN_DATA_OUT("wbcVpnDataOut"),
    ERICSSON_AMSTERDAM_VPN_DATA_IN("ericssonAmsterdamVpnDataIn"),
    ERICSSON_AMSTERDAM_VPN_DATA_OUT("ericssonAmsterdamVpnDataOut"),
    ERICSSON_STOCKHOLM_VPN_DATA_IN("ericssonStockholmVpnDataIn"),
    ERICSSON_STOCKHOLM_VPN_DATA_OUT("ericssonStockholmVpnDataOut"),

    COMMUNICATION_SYSTEM_ALARMS_COUNT("communicationSystemAlarmsCount"),
    QUALITY_OF_SERVICE_SYSTEM_ALARMS_COUNT("qualityOfServiceSystemAlarmsCount"),
    PROCESSING_SYSTEM_ALARMS_COUNT("processingSystemAlarmsCount"),
    EQUIPMENT_SYSTEM_ALARMS_COUNT("equipmentSystemAlarmsCount"),
    ENVIRONMENTAL_SYSTEM_ALARMS_COUNT("environmentalSystemAlarmsCount");

    private static final ApiUsageRecordKey[] JS_RECORD_KEYS = {JS_EXEC_COUNT};
    private static final ApiUsageRecordKey[] RE_RECORD_KEYS = {RE_EXEC_COUNT};
    private static final ApiUsageRecordKey[] DB_RECORD_KEYS = {STORAGE_DP_COUNT};
    private static final ApiUsageRecordKey[] TRANSPORT_RECORD_KEYS = {TRANSPORT_MSG_COUNT, TRANSPORT_DP_COUNT};
    private static final ApiUsageRecordKey[] EMAIL_RECORD_KEYS = {EMAIL_EXEC_COUNT};
    private static final ApiUsageRecordKey[] SMS_RECORD_KEYS = {SMS_EXEC_COUNT};
    private static final ApiUsageRecordKey[] ALARM_RECORD_KEYS = {CREATED_ALARMS_COUNT};

    @Getter
    private final ApiFeature apiFeature;
    @Getter
    private final String apiCountKey;
    @Getter
    private final String apiLimitKey;

    ApiUsageRecordKey(String apiCountKey) {
        this(null, apiCountKey, null);
    }

    ApiUsageRecordKey(ApiFeature apiFeature, String apiCountKey, String apiLimitKey) {
        this.apiFeature = apiFeature;
        this.apiCountKey = apiCountKey;
        this.apiLimitKey = apiLimitKey;
    }

    public static ApiUsageRecordKey[] getKeys(ApiFeature feature) {
        switch (feature) {
            case TRANSPORT:
                return TRANSPORT_RECORD_KEYS;
            case DB:
                return DB_RECORD_KEYS;
            case RE:
                return RE_RECORD_KEYS;
            case JS:
                return JS_RECORD_KEYS;
            case EMAIL:
                return EMAIL_RECORD_KEYS;
            case SMS:
                return SMS_RECORD_KEYS;
            case ALARM:
                return ALARM_RECORD_KEYS;
            default:
                return new ApiUsageRecordKey[]{};
        }
    }

}
