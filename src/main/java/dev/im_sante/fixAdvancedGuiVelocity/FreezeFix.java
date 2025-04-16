package dev.im_sante.fixAdvancedGuiVelocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "agfreezefixvelocity",
        name = "AG-FreezeFixVelocity",
        version = "1.0",
        description = "AdvancedGui Fix Freeze Velocity on server switch",
        authors = {"IM_SANTE"}
)
public class FreezeFix {

    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("advancedgui:fix");
    private final ProxyServer server;
    private final PluginContainer pluginContainer;
    private final Map<String, Integer> nextIds = new ConcurrentHashMap<>();

    @Inject
    public FreezeFix(ProxyServer server, Logger logger, PluginContainer pluginContainer) {
        this.server = server;
        this.pluginContainer = pluginContainer;
        server.getChannelRegistrar().register(CHANNEL);
        server.getEventManager().register(pluginContainer, this);
        logger.info("AG-FreezeFix Velocity enabled");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String playerName = in.readUTF();
        int nextId = in.readInt();
        nextIds.put(playerName, nextId);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        nextIds.remove(player.getUsername());
    }

    @Subscribe
    public void onServerChange(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        server.getScheduler().buildTask(pluginContainer, () -> {
            String name = player.getUsername();
            Integer nextId = nextIds.get(name);
            if (nextId != null) {
                player.getCurrentServer().ifPresent(serverConn -> {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF(name);
                    out.writeInt(nextId);
                    serverConn.getServer().sendPluginMessage(CHANNEL, out.toByteArray());
                });
            }
        }).delay(1, TimeUnit.MILLISECONDS).schedule();
    }
}
