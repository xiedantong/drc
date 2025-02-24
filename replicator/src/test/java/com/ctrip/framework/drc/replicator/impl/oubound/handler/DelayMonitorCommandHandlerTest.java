package com.ctrip.framework.drc.replicator.impl.oubound.handler;

import com.ctrip.framework.drc.core.driver.command.packet.monitor.DelayMonitorCommandPacket;
import com.ctrip.framework.drc.replicator.impl.inbound.event.ObservableLogEventHandler;
import com.ctrip.framework.drc.replicator.impl.inbound.event.ReplicatorLogEventHandler;
import com.ctrip.framework.drc.replicator.impl.monitor.DefaultMonitorManager;
import com.ctrip.framework.drc.replicator.impl.oubound.channel.ChannelAttributeKey;
import com.ctrip.xpipe.netty.commands.DefaultNettyClient;
import com.ctrip.xpipe.netty.commands.NettyClient;
import com.ctrip.xpipe.utils.Gate;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Attribute;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;

/**
 * Created by jixinwang on 2021/11/11
 */
public class DelayMonitorCommandHandlerTest {

    @Test
    public void testHandle() {
        Channel channel = Mockito.mock(Channel.class);
        ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);
        Attribute attribute = Mockito.mock(Attribute.class);
        ChannelAttributeKey channelAttributeKey = Mockito.mock(ChannelAttributeKey.class);
        Gate gate = Mockito.mock(Gate.class);
        Mockito.when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        Mockito.when(channel.closeFuture()).thenReturn(channelFuture);
        Mockito.when(channel.attr(Mockito.any())).thenReturn(attribute);
        Mockito.when(attribute.get()).thenReturn(channelAttributeKey);
        Mockito.when(channelAttributeKey.getGate()).thenReturn(gate);

        DefaultMonitorManager defaultMonitorManager = new DefaultMonitorManager("ut");
        ObservableLogEventHandler logEventHandler = new ReplicatorLogEventHandler(null, defaultMonitorManager, null);
        DelayMonitorCommandHandler delayMonitorCommandHandler = new DelayMonitorCommandHandler(logEventHandler, "test_dalcluster");
        delayMonitorCommandHandler.initialize();
        DelayMonitorCommandPacket monitorCommandPacket = new DelayMonitorCommandPacket("ntgxh", "test_dalcluster", "sha");

        NettyClient nettyClient = new DefaultNettyClient(channel);
        delayMonitorCommandHandler.handle(monitorCommandPacket, nettyClient);

        // 1) DefaultNettyClient 2) MonitorEventTask:run
        Mockito.verify(channelFuture, Mockito.atLeast(1)).addListener(Mockito.any());
        delayMonitorCommandHandler.dispose();
    }
}
