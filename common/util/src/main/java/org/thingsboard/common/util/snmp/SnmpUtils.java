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
package org.thingsboard.common.util.snmp;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.thingsboard.server.common.data.kv.DataType;

public class SnmpUtils {
    private SnmpUtils() {}

    public static Target createSnmpV2Target(String host, int port, String community, long timeout, int retries) {
        CommunityTarget target = new CommunityTarget();

        target.setAddress(new UdpAddress(String.format("%s/%s", host, port)));

        target.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv2c);
        target.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
        target.setCommunity(new OctetString(community));

        target.setTimeout(timeout);
        target.setRetries(retries);

        return target;
    }

    public static Target createSnmpV2Target(String host, int port, String community) {
        return createSnmpV2Target(host, port, community, 500, 0);
    }

    public static Variable toSnmpVariable(String value, DataType dataType) {
        dataType = dataType == null ? DataType.STRING : dataType;
        Variable variable;
        switch (dataType) {
            case LONG:
                try {
                    variable = new Integer32(Integer.parseInt(value));
                    break;
                } catch (NumberFormatException ignored) {
                }
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case JSON:
            default:
                variable = new OctetString(value);
        }
        return variable;
    }

}
