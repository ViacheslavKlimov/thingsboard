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
package org.thingsboard.server.transport.coap;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.EndpointContext;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.concurrent.ThreadLocalRandom;

import static org.eclipse.californium.core.coap.Message.MAX_MID;
import static org.eclipse.californium.core.coap.Message.NONE;


@Slf4j
public abstract class AbstractCoapTransportResource extends CoapResource {

    protected final CoapTransportContext transportContext;
    protected final TransportService transportService;

    public AbstractCoapTransportResource(CoapTransportContext context, String name) {
        super(name);
        this.transportContext = context;
        this.transportService = context.getTransportService();
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        processHandleGet(exchange);
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        processHandlePost(exchange);
    }

    protected abstract void processHandleGet(CoapExchange exchange);

    protected abstract void processHandlePost(CoapExchange exchange);

    protected void reportSubscriptionInfo(TransportProtos.SessionInfoProto sessionInfo, boolean hasAttributeSubscription, boolean hasRpcSubscription) {
        transportContext.getTransportService().process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(hasAttributeSubscription)
                .setRpcSubscription(hasRpcSubscription)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
    }

    protected void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.reportActivity(sessionInfo);
    }

    protected static TransportProtos.SessionEventMsg getSessionEventMsg(TransportProtos.SessionEvent event) {
        return TransportProtos.SessionEventMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .setEvent(event).build();
    }

    public static void respond(CoapTransportContext transportContext, Response response, int msgId, CoapExchange exchange, TransportProtos.SessionInfoProto sessionInfo) {
        response.setMID(msgId);
        transportContext.getRequestsAwaitingAck().put(msgId, new RequestInfo(sessionInfo, System.currentTimeMillis()));

        response.addMessageObserver(new TbCoapMessageObserver(msgId, id -> {
            transportContext.getRequestsAwaitingAck().remove(id);

        }, null));
        response.addMessageObserver(new CoapRequestFailureMessageObserver(() -> {
            RequestInfo requestInfo = transportContext.getRequestsAwaitingAck().remove(msgId);
            if (requestInfo != null) {
                transportContext.getApiUsageReportClient().report(TransportService.getTenantId(sessionInfo),
                        TransportService.getCustomerId(sessionInfo), ApiUsageRecordKey.FAILED_DOWNLINK_MSG_COUNT);
            }
        }));

        transportContext.getApiUsageReportClient().report(TransportService.getTenantId(sessionInfo),
                TransportService.getCustomerId(sessionInfo), ApiUsageRecordKey.DOWNLINK_MSG_COUNT);
        exchange.respond(response);
    }

    public static void respond(CoapTransportContext transportContext, Response response, CoapExchange exchange, TransportProtos.SessionInfoProto sessionInfo) {
        respond(transportContext, response, getNextMsgId(), exchange, sessionInfo);
    }

    public void respond(Response response, CoapExchange exchange, TransportProtos.SessionInfoProto sessionInfo) {
        respond(transportContext, response, getNextMsgId(), exchange, sessionInfo);
    }

    @Data
    static class RequestInfo {
        private final TransportProtos.SessionInfoProto sessionInfo;
        private final long requestTime;
    }

    @Data
    static class RespondResult {
        private final int msgId;
    }

    protected static int getNextMsgId() {
        return ThreadLocalRandom.current().nextInt(NONE, MAX_MID + 1);
    }

    @RequiredArgsConstructor
    public static class CoapRequestFailureMessageObserver implements MessageObserver {
        private final Runnable onError;

        @Override
        public void onRetransmission() {

        }

        @Override
        public void onResponse(Response response) {

        }

        @Override
        public void onAcknowledgement() {

        }

        @Override
        public void onReject() {
            onError.run();
        }

        @Override
        public void onTimeout() {
            onError.run();
        }

        @Override
        public void onCancel() {
            onError.run();
        }

        @Override
        public void onReadyToSend() {

        }

        @Override
        public void onConnecting() {

        }

        @Override
        public void onDtlsRetransmission(int flight) {

        }

        @Override
        public void onSent(boolean retransmission) {

        }

        @Override
        public void onSendError(Throwable error) {
            onError.run();
        }

        @Override
        public void onContextEstablished(EndpointContext endpointContext) {

        }

        @Override
        public void onComplete() {

        }
    }
}
