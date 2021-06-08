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
package org.thingsboard.server.service.sms.rest_api;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.sms.AbstractSmsSender;
import org.thingsboard.server.common.data.sms.SmsSendRequest;
import org.thingsboard.server.common.data.sms.SmsSendResponse;
import org.thingsboard.server.common.data.sms.config.TbSmppGatewaySmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.exception.SmsException;
import org.thingsboard.server.common.data.sms.exception.SmsSendException;

import java.io.IOException;
import java.time.Duration;

public class TbSmppGatewaySmsSender extends AbstractSmsSender {
    private final String url;
    private final RestTemplate restTemplate;

    public TbSmppGatewaySmsSender(TbSmppGatewaySmsProviderConfiguration config) {
        this.url = StringUtils.stripEnd(config.getUrl(), "/").concat("/api/sms");
        this.restTemplate = new RestTemplateBuilder()
                .basicAuthentication(config.getUsername(), config.getPassword())
                .setConnectTimeout(Duration.ofSeconds(15))
                .errorHandler(new ErrorHandler())
                .build();
    }

    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        numberTo = validatePhoneNumber(numberTo);
        message = prepareMessage(message);

        ResponseEntity<SmsSendResponse> response = restTemplate.postForEntity(url, new SmsSendRequest(numberTo, message), SmsSendResponse.class);

        return response.getBody().getMessageSegments();
    }

    private static class ErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            HttpStatus httpStatus = response.getStatusCode();
            throw new SmsSendException(httpStatus.value() + " " + httpStatus.getReasonPhrase());
        }
    }

}
