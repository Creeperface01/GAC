package cz.creeperface.nukkit.gac

import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.event.Listener
import cn.nukkit.event.level.LevelLoadEvent
import cn.nukkit.event.level.LevelUnloadEvent
import cn.nukkit.level.Level
import cn.nukkit.plugin.PluginBase
import cn.nukkit.utils.Config
import cn.nukkit.utils.PluginException
import cn.nukkit.utils.TextFormat
import cz.creeperface.nukkit.gac.listener.PlayerListener
import cz.creeperface.nukkit.gac.listener.SynapseListener
import cz.creeperface.nukkit.gac.punishment.DefaultPunishmentManager
import cz.creeperface.nukkit.gac.punishment.PunishmentManager
import cz.creeperface.nukkit.gac.utils.*
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import java.io.File
import java.util.*
import kotlin.math.pow

/**
 * Created by CreeperFace on 10.6.2017.
 */
class GTAnticheat : PluginBase(), Listener {

    var kickQueue = HashMap<Long, String>()
    var banQueue = HashMap<Long, String>()

    var punishmentManager: PunishmentManager = DefaultPunishmentManager(this)

    private val enabledLevels = IntOpenHashSet()

    override fun onLoad() {
        instance = this

        if (!KotlinLibDownloader.check(this)) {
            throw PluginException("KotlinLib could not be found")
        }
    }

    override fun onEnable() {
        instance = this
        PlayerListener(this)
        this.server.scheduler.scheduleRepeatingTask(NoCheatTask(this), 20)

        if (this.server.pluginManager.getPlugin("SynapseAPI") != null) {
            SynapseListener(this)
        }

        loadConf()
        loadMessages()

        registerEvent(this, this, LevelLoadEvent::class.java, { onLevelLoad(it.level) })
        registerEvent(this, this, LevelUnloadEvent::class.java, { onLevelUnload(it) })

        this.server.levels.values.forEach { level -> onLevelLoad(level) }
        logo()

        Messages.translate("kick_player", "fly", "test")
    }



    fun doKickCheck(data: ACData, p: Player) {
        if (!conf.kick) {
            return
        }

        var reason: String? = null
        val cheatData = data.antiCheatData

        //System.out.println("speedPoints: "+cheatData.speedPoints);
        /*if (cheatData.speedPoints > 40) {
            reason = "speed";
        }  else*/
        if (cheatData.flyPoints > 5 || cheatData.inAirPoints > 3) {
            //reason = "fly";
        } else if (cheatData.nukerPoints > 5) {
            reason = "nuker"
        } else if (cheatData.glidePoints > 5) {
            reason = "glide"
        }

        if (cheatData.killAuraPoints > 0) {
            reason = "kill aura"
        }

        reason?.let {
            kickQueue[p.id] = reason
        }
    }

    private fun onLevelLoad(level: Level) {
        val excluded = conf.excludedLevels.contains(level.folderName)
        if ((!conf.active && excluded) || (conf.active && !excluded)) {
            enabledLevels.add(level.id)
        }
    }

    private fun onLevelUnload(e: LevelUnloadEvent) {
        enabledLevels.remove(e.level.id)
    }

    fun isLevelActive(level: Level) = enabledLevels.contains(level.id)

    /*@EventHandler
    public void onDataPacketReceive(DataPacketReceiveEvent e) {
        if(e.getPlayer().getProtocol() <= 113) {
            if (e.getPacket().pid() == org.itxtech.synapseapi.multiprotocol.protocol11.protocol.ProtocolInfo.PLAYER_ACTION_PACKET) {
                if (((org.itxtech.synapseapi.multiprotocol.protocol11.protocol.PlayerActionPacket) e.getPacket()).action == PlayerActionPacket.ACTION_JUMP) {
                    PlayerData data = MTCore.getInstance().getPlayerData(e.getPlayer());
                    long time = System.currentTimeMillis();

                    data.acData.antiCheatData.lastPacketJump = time;
                    //System.out.println("jump 0");

                    if (data.acData.antiCheatData.getLastGroundPos().distance(e.getPlayer()) < 0.4) {
                        onJump(e.getPlayer(), data);
                        //System.out.println("jump");
                    }
                    //}

                    MotionData motionData = data.acData.getMotionData();
                    if (!motionData.isEmpty() && !motionData.ground) {
                        long timeY = motionData.timeY;

                        if (time > timeY && time > motionData.time && motionData.groundTime != -1 && time - motionData.groundTime > 1500) {
                            //System.out.println("jump ground");
                            motionData.ground = true;
                        }
                    }
                }
            }
        } else {
            if (e.getPacket().pid() == ProtocolInfo.PLAYER_ACTION_PACKET) {
                if (((PlayerActionPacket) e.getPacket()).action == PlayerActionPacket.ACTION_JUMP) {
                    PlayerData data = MTCore.getInstance().getPlayerData(e.getPlayer());
                    long time = System.currentTimeMillis();

                    data.acData.antiCheatData.lastPacketJump = time;
                    //System.out.println("jump 0");

                    if (data.acData.antiCheatData.getLastGroundPos().distance(e.getPlayer()) < 0.4) {
                        onJump(e.getPlayer(), data);
                        //System.out.println("jump");
                    }
                    //}

                    MotionData motionData = data.acData.getMotionData();
                    if (!motionData.isEmpty() && !motionData.ground) {
                        long timeY = motionData.timeY;

                        if (time > timeY && time > motionData.time && motionData.groundTime != -1 && time - motionData.groundTime > 1500) {
                            //System.out.println("jump ground");
                            motionData.ground = true;
                        }
                    }
                }
            }
        }
    }*/

    @Suppress("UNCHECKED_CAST")
    private fun loadMessages() {
        saveResource("messages.yml")
        val cfg = Config(File(dataFolder, "messages.yml"))

        Messages.load(cfg.all as Map<String, String>)
    }

    private fun loadConf() {
        saveDefaultConfig()
        val cfg = config

        val version = cfg.getInt("version")

        if (version < Configuration.VERSION) {
            logger.warning("You are using an older config version, some properties are probably missing")
        } else if (version > Configuration.VERSION) {
            logger.warning("You are using config from a newer GAC version, some properties might be incompatible")
        }

        val default = cfg.getBoolean("default_active")
        val excluded = cfg.getStringList("excluded_levels").toSet()
        val kick = cfg.getBoolean("kick_players")
        val checkOps = cfg.getBoolean("check_ops")
        val enableElytra = cfg.getBoolean("enable_elytra", false)
        val maxDistance = cfg.getDouble("max_movement_distance", 10.0).pow(2)

        val settings = cfg.getSection("settings")
        val spamDelay = settings.getInt("spam_delay")
        val hitRange = settings.getDouble("hit_range")

        val map = mutableMapOf<Int, Boolean>()
        val checks = cfg.getSection("checks")

        map[CheckType.FLY.ordinal] = checks.getBoolean("fly")
        map[CheckType.GLIDE.ordinal] = checks.getBoolean("glide")
        map[CheckType.NOCLIP.ordinal] = checks.getBoolean("noclip")
        map[CheckType.NUKER.ordinal] = checks.getBoolean("nuker")
        map[CheckType.REACH.ordinal] = checks.getBoolean("reach")
        map[CheckType.SPAM.ordinal] = checks.getBoolean("spam")
        map[CheckType.SPEED.ordinal] = checks.getBoolean("speed")
        map[CheckType.SPEEDMINE.ordinal] = checks.getBoolean("speedmine")
        map[CheckType.TELEPORT.ordinal] = checks.getBoolean("teleport")
        map[CheckType.AIMBOT.ordinal] = checks.getBoolean("aimbot")
        map[CheckType.BHOP.ordinal] = checks.getBoolean("bhop")

        conf = Configuration(
                default,
                excluded,
                kick,
                checkOps,
                map,
                spamDelay,
                hitRange,
                enableElytra,
                maxDistance
        )

        if (conf.enabled(CheckType.FLY) && server.allowFlight) {
            val fly = Server::class.java.getDeclaredField("getAllowFlight")
            fly.isAccessible = true
            fly.set(this.server, false)
            logger.warning("Disabling server flight option because of GAC fly check enabled")
        }
    }

    private fun logo() {
        logger.info(" ")
        logger.info("${TextFormat.YELLOW} ██████╗  █████╗  ██████╗")
        logger.info("${TextFormat.YELLOW}██╔════╝ ██╔══██╗██╔════╝")
        logger.info("${TextFormat.YELLOW}██║  ███╗███████║██║     ")
        logger.info("${TextFormat.YELLOW}██║   ██║██╔══██║██║     ")
        logger.info("${TextFormat.YELLOW}╚██████╔╝██║  ██║╚██████╗")
        logger.info("${TextFormat.YELLOW} ╚═════╝ ╚═╝  ╚═╝ ╚═════╝")
        logger.info(" ")
        logger.info("${TextFormat.YELLOW}       GAC enabled       ")
    }

    companion object {

        @JvmStatic
        lateinit var instance: GTAnticheat
            private set

        val DEBUG = false

        val DEBUG_CREATIVE = false

        lateinit var conf: Configuration
            private set
    }
}
