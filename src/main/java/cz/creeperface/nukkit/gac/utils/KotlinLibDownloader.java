package cz.creeperface.nukkit.gac.utils;

import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import lombok.Cleanup;
import lombok.experimental.UtilityClass;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @author CreeperFace
 */
@UtilityClass
public class KotlinLibDownloader {

    public static boolean check(final Plugin plugin) {
        final Server server = plugin.getServer();

        if (server.getPluginManager().getPlugin("KotlinLib") != null) {
            return true;
        }

        plugin.getLogger().info("Downloading KotlinLib...");

        final String pluginPath = server.getFilePath() + "/plugins/KotlinLib.jar";

        try {
            final URL website = new URL("https://www.dropbox.com/s/stdjc441x1283at/KotlinLib.jar?dl=1");
            @Cleanup final ReadableByteChannel rbc = Channels.newChannel(website.openStream());

            @Cleanup final FileOutputStream fos = new FileOutputStream(pluginPath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (final Exception e) {
            server.getLogger().logException(e);
            return false;
        }

        plugin.getLogger().info("KotlinLib successfully downloaded");
        return server.getPluginManager().loadPlugin(pluginPath) != null;
    }
}
