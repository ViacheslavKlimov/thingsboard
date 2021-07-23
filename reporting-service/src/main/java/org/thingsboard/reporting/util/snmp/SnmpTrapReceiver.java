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
package org.thingsboard.reporting.util.snmp;

import org.snmp4j.*;
import org.snmp4j.mp.*;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.*;
import org.snmp4j.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class SnmpTrapReceiver implements CommandResponder {
    private MultiThreadedMessageDispatcher dispatcher;
    private Snmp snmp = null;
    private Address listenAddress;
    private ThreadPool threadPool;
    private int n = 0;
    private long start = -1;

    public static void main(String[] args) {
        new SnmpTrapReceiver().run();
    }

    private void run() {
        try {
            init();
            snmp.addCommandResponder(this);
            System.out.println("Initialized SNMP trap receiver");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void init() throws UnknownHostException, IOException {
        threadPool = ThreadPool.create("Trap", 10);
        dispatcher = new MultiThreadedMessageDispatcher(threadPool,
                new MessageDispatcherImpl());

        //TRANSPORT
        listenAddress = GenericAddress.parse(System.getProperty(
                "snmp4j.listenAddress", "udp:0.0.0.0/1620"));  //SET THIS
        TransportMapping<?> transport;
        if (listenAddress instanceof UdpAddress) {
            transport = new DefaultUdpTransportMapping(
                    (UdpAddress) listenAddress);
        } else {
            transport = new DefaultTcpTransportMapping(
                    (TcpAddress) listenAddress);
        }

        //V3 SECURITY
        USM usm = new USM(
                SecurityProtocols.getInstance().addDefaultProtocols(),
                new OctetString(MPv3.createLocalEngineID()), 0);

        SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES192());
        SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES256());
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

        usm.setEngineDiscoveryEnabled(true);

        SecurityModels.getInstance().addSecurityModel(usm);

        snmp = new Snmp(dispatcher, transport);
        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3(usm));


        String username = "username";         // SET THIS
        String authpassphrase = "authpassphrase";   // SET THIS
        String privacypassphrase = "privacypassphrase";   // SET THIS

        snmp.getUSM().addUser(    // SET THE SECURITY PROTOCOLS HERE
                new OctetString(username),
                new UsmUser(new OctetString(username),AuthMD5.ID, new OctetString(
                        authpassphrase), PrivAES128.ID, new OctetString(privacypassphrase)));

        snmp.listen();
    }

    public void processPdu(CommandResponderEvent crEvent) {
        PDU pdu = crEvent.getPDU();
        if (pdu.getType() == PDU.V1TRAP) {

            PDUv1 pduV1 = (PDUv1) pdu;
            System.out.println("");
            System.out.println("===== NEW SNMP 1 TRAP RECEIVED ====");
            System.out.println("agentAddr " + pduV1.getAgentAddress().toString());
            System.out.println("enterprise " + pduV1.getEnterprise().toString());
            System.out.println("timeStam" + String.valueOf(pduV1.getTimestamp()));
            System.out.println("genericTrap"+ String.valueOf(pduV1.getGenericTrap()));
            System.out.println("specificTrap " + String.valueOf(pduV1.getSpecificTrap()));
            System.out.println("snmpVersion " + String.valueOf(PDU.V1TRAP));
            System.out.println("communityString " + new String(crEvent.getSecurityName()));

        } else if (pdu.getType() == PDU.TRAP) {
            System.out.println("");
            System.out.println("===== NEW SNMP 2/3 TRAP RECEIVED ====");

            System.out.println("errorStatus " + String.valueOf(pdu.getErrorStatus()));
            System.out.println("errorIndex "+ String.valueOf(pdu.getErrorIndex()));
            System.out.println("requestID " +String.valueOf(pdu.getRequestID()));
            System.out.println("snmpVersion " + String.valueOf(PDU.TRAP));
            System.out.println("communityString " + new String(crEvent.getSecurityName()));

        }

        Vector<? extends VariableBinding> varBinds = pdu.getVariableBindings();
        if (varBinds != null && !varBinds.isEmpty()) {
            Iterator<? extends VariableBinding> varIter = varBinds.iterator();

            StringBuilder resultset = new StringBuilder();
            resultset.append("-----");
            while (varIter.hasNext()) {
                VariableBinding vb = varIter.next();

                String syntaxstr = vb.getVariable().getSyntaxString();
                int syntax = vb.getVariable().getSyntax();
                System.out.println( "OID: " + vb.getOid());
                System.out.println("Value: " +vb.getVariable());
                System.out.println("syntaxstring: " + syntaxstr );
                System.out.println("syntax: " + syntax);
                System.out.println("------");
            }


        }
        System.out.println("==== TRAP END ===");
        System.out.println("");
    }
}
