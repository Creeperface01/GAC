package cz.creeperface.nukkit.gac.checks

/**
 * Created by CreeperFace on 20. 11. 2016.
 */
class PacketCountCheck {

    /*public static void run(PlayerMoveEvent e, PlayerData data) {
        PacketsData pData = data.packetsData;
        pData.count++;

        long time = System.currentTimeMillis();
        long currentTick = Server.getInstance().getTick();

        if (time - pData.lastUpdate > 1000) {
            pData.count = 0;
            pData.lastUpdate = System.currentTimeMillis();
            pData.lastTick = currentTick;
            pData.lastPos = e.getPlayer().clone();
            pData.revertNumber = 0;
        } else {
            int maxPacketCount = Math.min(10, ((int) (time - pData.lastUpdate) / 50) + 2);

            if (pData.lastUpdate - (time - pData.lastLagDuration) < 70 && pData.lastLagTick >= pData.lastTick) {
                maxPacketCount = 25 + (int) pData.lastLagDuration / 50;
            }

            if (pData.count > maxPacketCount) {
                e.setTo(data.packetsData.lastPos);
                pData.lastUpdate = System.currentTimeMillis();
                pData.count = 0;
                pData.revertNumber++;

                if (data.packetsData.revertNumber > 4) {
                    //data.antiCheatData.points = 20;
                    //data.antiCheatData.reason = "speed";
                }
            }
        }
    }*/

    inner class Entry {

        var revertCount = 0
        var lastCheckTick = 0
        var lastCheckTime: Long = 0
        var packetCount = 0

    }
}
