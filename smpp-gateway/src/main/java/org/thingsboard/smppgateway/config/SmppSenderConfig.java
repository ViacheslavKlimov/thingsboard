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
package org.thingsboard.smppgateway.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.smpp.pdu.BindReceiver;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindTransciever;
import org.smpp.pdu.BindTransmitter;

import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmppSenderConfig {
    private SmppVersion protocolVersion;

    private String host;
    private Integer port;
    private String systemId;
    private String password;
    private String systemType;
    private SmppBindType bindType;
    private String serviceType;
    private String sourceAddress;

    private Byte ton;
    private Byte npi;
    private Byte destinationTon;
    private Byte destinationNpi;
    private String addressRange;

    private Byte encoding;


    public enum SmppVersion {
        V_3_3("3.3", org.smpp.Data.SMPP_V33),
        V_3_4("3.4", org.smpp.Data.SMPP_V34);

        private final String name;
        private final byte code;

        SmppVersion(String name, int code) {
            this.name = name;
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }

        public static SmppVersion forName(String name) {
            return Arrays.stream(SmppVersion.values())
                    .filter(smppVersion -> smppVersion.name.equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No SMPP version for name '" + name + "'"));
        }
    }

    public enum SmppBindType {
        TX {
            @Override
            public BindRequest newBindRequest() {
                return new BindTransmitter();
            }
        },
        RX {
            @Override
            public BindRequest newBindRequest() {
                return new BindReceiver();
            }
        },
        TRX {
            @Override
            public BindRequest newBindRequest() {
                return new BindTransciever();
            }
        };

        public abstract BindRequest newBindRequest();
    }

}
