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
package org.thingsboard.server.transport.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteOrder;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
public class TcpTransportServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final String LITTLE_ENDIAN_BYTE_ORDER = "LITTLE_ENDIAN";

    private final TcpTransportContext ctx;

    public TcpTransportServerInitializer(TcpTransportContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        try {
            ByteOrder byteOrder = LITTLE_ENDIAN_BYTE_ORDER.equals(ctx.getByteOrder()) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            LengthFieldBasedFrameDecoder framer = new LengthFieldBasedFrameDecoder(
                    byteOrder,
                    ctx.getMaxFrameLength(),
                    ctx.getLengthFieldOffset(),
                    ctx.getLengthFieldLength(),
                    ctx.getLengthAdjustment(),
                    ctx.getInitialBytesToStrip(),
                    ctx.isFailFast()
            );
            ChannelPipeline channelPipeline = socketChannel.pipeline();
            channelPipeline.addLast("tcpByteDecoder", framer);

            TcpTransportHandler handler = new TcpTransportHandler(ctx);
            channelPipeline.addLast(handler);
        } catch (Exception e) {
            log.error("Init Channel Exception: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
