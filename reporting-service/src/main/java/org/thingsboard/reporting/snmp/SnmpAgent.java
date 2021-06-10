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
package org.thingsboard.reporting.snmp;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServerLookupEvent;
import org.snmp4j.agent.MOServerLookupListener;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.DefaultMOMutableRow2PC;
import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.DefaultMOTableRow;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOGroupImpl;
import org.snmp4j.agent.mo.MOMutableColumn;
import org.snmp4j.agent.mo.MOMutableTableModel;
import org.snmp4j.agent.mo.MOMutableTableRow;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableModel;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.MOTableRowEvent;
import org.snmp4j.agent.mo.MOTableRowListener;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.agent.mo.ext.StaticMOGroup;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB.SnmpCommunityEntryRow;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.thingsboard.common.util.snmp.SnmpUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
public class SnmpAgent extends BaseAgent {
    private final int port;
    private final String community;

    public SnmpAgent(int port) {
        this(port, "public");
    }

    public SnmpAgent(int port, String community) {
        super(new File("conf.agent"), new File("bootCounter.agent"), new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
        this.port = port;
        this.community = community;
    }

    public void start() throws IOException {
        init();

        addShutdownHook();
        getServer().addContext(new OctetString(community));

        finishInit();

        run();
        sendColdStartNotification();
    }

    @Override
    protected void initTransportMappings() throws IOException {
        transportMappings = new TransportMapping<?>[]{new DefaultUdpTransportMapping(new UdpAddress(port))};
    }

    public void registerVariable(String oid, Supplier<Object> valueSupplier) {
        registerVariable(oid, valueSupplier, null);
    }

    public void registerVariable(String oid, Supplier<Object> valueSupplier, Object defaultValue) {
        MOScalar<Variable> mo = registerMO(oid, SnmpUtils.toSnmpVariable(defaultValue));
        server.addLookupListener(new MOServerLookupListener() {
            @Override
            public void lookupEvent(MOServerLookupEvent event) {
            }

            @Override
            public void queryEvent(MOServerLookupEvent event) {
                Object value = valueSupplier.get();
                if (value == null) {
                    value = defaultValue;
                }
                mo.setValue(SnmpUtils.toSnmpVariable(value));
            }
        }, mo);
    }

    public void registerVariables(Map<Integer, Supplier<Object>> variables, String baseOid) throws DuplicateRegistrationException {
        MOMutableTableModel<MOMutableTableRow> model = new DefaultMOMutableTableModel<>();
        MOColumn<Variable> column = new MOMutableColumn<>(
                Integer.parseInt(StringUtils.substringAfterLast(baseOid, ".")),
                SMIConstants.SYNTAX_NULL, MOAccessImpl.ACCESS_READ_ONLY, Null.instance
        );
        DefaultMOTable<?, ?, ?> table = new DefaultMOTable<>(
                new OID(StringUtils.substringBeforeLast(baseOid, ".")),
                new MOTableIndex(new MOTableSubIndex[]{new MOTableSubIndex(SMIConstants.SYNTAX_NULL)}, false),
                new MOColumn[]{column}, model
        );

        variables.keySet().forEach(id -> {
            MOMutableTableRow row = new DefaultMOMutableRow2PC(new OID(id.toString()), new Variable[]{Null.instance});
            model.addRow(row);
        });

        table.setVolatile(true);
        server.register(table, null);

        server.addLookupListener(new MOServerLookupListener() {
            @Override
            public void lookupEvent(MOServerLookupEvent event) {
            }

            @Override
            public void queryEvent(MOServerLookupEvent event) {
                if (event.getQuery().getLowerBound().equals(new OID(baseOid))) {
                    return;
                }

                int[] oid = event.getQuery().getLowerBound().getValue();
                int id = oid[oid.length - 1];

                Supplier<Object> valueSupplier = variables.get(id);
                Object value = valueSupplier.get();
                model.getRow(new OID(String.valueOf(id))).setValue(0, SnmpUtils.toSnmpVariable(value));
            }
        }, table);
    }

    @SneakyThrows
    private MOScalar<Variable> registerMO(String oid, Variable variable) {
        MOScalar<Variable> mo = new MOScalar<>(new OID(oid), MOAccessImpl.ACCESS_READ_ONLY, variable);
        server.register(mo, null);
        return mo;
    }

    protected void addViews(VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(community),
                new OctetString("v1v2group"), StorageType.nonVolatile);

        vacm.addAccess(new OctetString("v1v2group"), new OctetString(community),
                SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT,
                new OctetString("fullReadView"), new OctetString("fullWriteView"),
                new OctetString("fullNotifyView"), StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    }

    @Override
    protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[]{
                new OctetString(community), // community name
                new OctetString(community), // security name
                getAgent().getContextEngineID(), // local engine ID
                new OctetString(community), // default context name
                new OctetString(), // transport tag
                new Integer32(StorageType.nonVolatile), // storage type
                new Integer32(RowStatus.active) // row status
        };
        SnmpCommunityEntryRow row = communityMIB.getSnmpCommunityEntry().createRow(
                new OctetString("public2public").toSubIndex(true), com2sec
        );
        communityMIB.getSnmpCommunityEntry().addRow(row);
    }

    @Override
    protected void registerManagedObjects() {}

    @Override
    protected void unregisterManagedObjects() {}

    @Override
    protected void addNotificationTargets(SnmpTargetMIB targetMIB, SnmpNotificationMIB notificationMIB) {}

    @Override
    protected void addUsmUser(USM usm) {}

    public static class SnmpVariable {
        private final MOScalar<Variable> mo;

        protected SnmpVariable(MOScalar<Variable> mo) {
            this.mo = mo;
        }

        public void setValue(Object value) {
            mo.setValue(SnmpUtils.toSnmpVariable(value));
        }
    }

}
