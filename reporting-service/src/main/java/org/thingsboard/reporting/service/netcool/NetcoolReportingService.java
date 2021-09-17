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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.snmp.SnmpUtils;
import org.thingsboard.server.common.data.stats.SystemAlarm;

import javax.annotation.PostConstruct;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NetcoolReportingService {
    @Value("${netcool.snmp.host}")
    private String snmpHost;
    @Value("${netcool.snmp.port}")
    private Integer snmpPort;
    @Value("${netcool.snmp.community}")
    private String snmpCommunity;

    @Value("${netcool.alarms.trap_base_oid}")
    private String alarmTrapBaseOid;
    @Value("${netcool.binding_port}")
    private Integer bindingPort;

    private Target target;
    private Snmp snmp;

    @PostConstruct
    private void init() throws IOException {
        this.target = SnmpUtils.createSnmpV2Target(snmpHost, snmpPort, snmpCommunity);
        this.snmp = new Snmp(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/" + bindingPort)));
    }

    public void onAlarm(SystemAlarm alarm) {
        log.info("Received new alarm: {}", alarm);
        reportAlarm(alarm);
    }

    private void reportAlarm(SystemAlarm alarm) {
        try {
            snmp.send(toTrapPdu(alarm), target);
        } catch (Exception e) {
            log.error("Failed to report alarm to Netcool: {}", alarm, e);
        }
    }

    private PDU toTrapPdu(SystemAlarm alarm) {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        pdu.add(new VariableBinding(new OID(getAlarmOid(alarm)), SnmpUtils.toSnmpVariable(mapToString(alarm))));
        return pdu;
    }

    private String getAlarmOid(SystemAlarm alarm) {
        return String.format("%s.%d.%d", alarmTrapBaseOid, alarm.getSeverity().getId(), alarm.getCategory().getId());
    }

    private String mapToString(SystemAlarm alarm) {
        return alarm.getTitle();
    }

}
