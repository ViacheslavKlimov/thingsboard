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
package org.thingsboard.reporting.service.netcool;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.snmp.SnmpUtils;
import org.thingsboard.server.common.adaptor.JsonConverter;

import java.text.ParseException;

//@Service
@RequiredArgsConstructor
@Slf4j
public class NetcoolReportingService {
    @Value("${netcool.snmp.host}")
    private String snmpHost;
    @Value("${netcool.snmp.port}")
    private Integer snmpPort;
    @Value("${netcool.snmp.community}")
    private String snmpCommunity = "public";
    private static final String alarmOidTemplate = "1.3.6.1.2.3.4.%d.%d";

    private Target target;

    private final Snmp snmp = new Snmp();

//    @PostConstruct
    private void init() {
        target = SnmpUtils.createSnmpV2Target(snmpHost, snmpPort, snmpCommunity);
    }

    @SneakyThrows()
    public void report(NetcoolAlarm alarm) {
        try {
            snmp.send(toTrapPdu(alarm), target);
        } catch (Exception e) {
            log.error("Failed to report alarm to Netcool: {}", ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private PDU toTrapPdu(NetcoolAlarm alarm) throws ParseException {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        pdu.add(new VariableBinding(getOidForAlarm(alarm), mapToString(alarm)));
        return pdu;
    }

    private OID getOidForAlarm(NetcoolAlarm alarm) {
        return new OID(String.format(alarmOidTemplate, alarm.getSeverity().getId(), alarm.getType().getId()));
    }

    private String mapToString(NetcoolAlarm alarm) {
        return JsonConverter.toJson(alarm);
    }

    @Data
    public static class NetcoolAlarm {
        private Severity severity;
        private Type type;
        private String description;

        public enum Severity {
            CLEARED(1),
            MINOR(2),
            WARNING(3),
            MAJOR(4),
            CRITICAL(5);

            private final int id;

            Severity(int id) {
                this.id = id;
            }

            public int getId() {
                return id;
            }
        }

        public enum Type {
            COMMUNICATION(1),
            QUALITY_OF_SERVICE(2),
            PROCESSING(3),
            EQUIPMENT(4),
            ENVIRONMENTAL(5);

            private final int id;

            Type(int id) {
                this.id = id;
            }

            public int getId() {
                return id;
            }
        }
    }

}
