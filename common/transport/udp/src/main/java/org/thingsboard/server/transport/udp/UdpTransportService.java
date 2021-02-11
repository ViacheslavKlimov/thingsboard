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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service("UdpTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.udp.enabled}'=='true')")
@Slf4j
public class UdpTransportService {

    @Value("${transport.udp.bind_address}")
    private String host;
    @Value("${transport.udp.bind_port}")
    private Integer port;

    @Value("${transport.udp.netty.worker_group_thread_count}")
    private Integer workerGroupThreadCount;

    @Value("${transport.udp.netty.so_broadcast}")
    private boolean broadcast;
    @Value("${transport.udp.netty.so_rcv_buf}")
    private Integer rcvBuf;

    @Autowired
    private UdpTransportContext context;

    private Channel serverChannel;

    private EventLoopGroup workerGroup;

    @PostConstruct
    public void init() throws Exception {
        log.info("Starting UDP transport...");

        workerGroup = new NioEventLoopGroup(workerGroupThreadCount);

        Bootstrap server = new Bootstrap().group(workerGroup);
        server.channel(NioDatagramChannel.class)
                .handler(new UdpTransportServerInitializer(context))
                .option(ChannelOption.SO_BROADCAST, broadcast)
                .option(ChannelOption.SO_RCVBUF, rcvBuf * 1024);

        serverChannel = server.bind(host, port).sync().channel();
        log.info("UDP transport started!");
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Stopping UDP transport!");
        try {
            serverChannel.close().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
        log.info("UDP transport stopped!");
    }
}
