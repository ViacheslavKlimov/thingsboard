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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.smppgateway.config.SmppSenderConfig.SmppBindType;
import org.thingsboard.smppgateway.config.SmppSenderConfig.SmppVersion;
import org.thingsboard.smppgateway.service.SmppSmsSender;

@Configuration
public class SmppConfig {
    @Value("${smpp.version}")
    private String protocolVersion;

    @Value("${smpp.host}")
    private String host;
    @Value("${smpp.port}")
    private Integer port;
    @Value("${smpp.system_id}")
    private String systemId;
    @Value("${smpp.password}")
    private String password;
    @Value("${smpp.bind_type}")
    private SmppBindType bindType;

    @Value("${smpp.system_type}")
    private String systemType;
    @Value("${smpp.service_type}")
    private String serviceType;
    @Value("${smpp.source_address}")
    private String sourceAddress;

    @Value("${smpp.ton}")
    private Byte ton;
    @Value("${smpp.npi}")
    private Byte npi;
    @Value("${smpp.destination_ton}")
    private Byte destinationTon;
    @Value("${smpp.destination_npi}")
    private Byte destinationNpi;
    @Value("${smpp.address_range}")
    private String addressRange;

    @Value("${smpp.encoding}")
    private Byte encoding;

    @Bean
    public SmppSmsSender smppSmsSender() {
        SmppSenderConfig senderConfig = SmppSenderConfig.builder()
                .protocolVersion(SmppVersion.forName(protocolVersion))
                .host(host)
                .port(port)
                .systemId(systemId)
                .password(password)
                .systemType(systemType)
                .bindType(bindType)
                .serviceType(serviceType)
                .sourceAddress(sourceAddress)
                .ton(ton)
                .npi(npi)
                .destinationTon(destinationTon)
                .destinationNpi(destinationNpi)
                .addressRange(addressRange)
                .encoding(encoding)
                .build();
        return new SmppSmsSender(senderConfig);
    }

}
