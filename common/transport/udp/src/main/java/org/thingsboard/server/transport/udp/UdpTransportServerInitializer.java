/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;

@Slf4j
public class UdpTransportServerInitializer extends ChannelInitializer<NioDatagramChannel> {

    private final UdpTransportContext ctx;

    public UdpTransportServerInitializer(UdpTransportContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void initChannel(final NioDatagramChannel channel) {
        try {
            channel.pipeline()
                    .addLast("datagramToByteDecoder", new UdpMsgDecoder(msg -> toByteArray(msg.content())) {
                    })
                    .addLast("udpByteHandler", new UdpTransportHandler(ctx));
        } catch (Exception e) {
            log.error("Init Channel Exception: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @AllArgsConstructor
    private static class UdpMsgDecoder extends MessageToMessageDecoder<DatagramPacket> {

        private Function<DatagramPacket, byte[]> transformer;

        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
            try {
                out.add(transformer.apply(msg));
            } catch (Exception e) {
                log.error("[{}] Exception during of decoding message", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    public byte[] toByteArray(ByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

}
