package seml.battleroyal2

import BattleroyalTabCompleter
import com.sun.tools.javac.tree.TreeInfo.args
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import jdk.internal.net.http.common.Utils.remaining
import net.kyori.adventure.bossbar.BossBar.bossBar
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.Location
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import kotlin.math.pow
import net.kyori.adventure.title.Title
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.scheduler.BukkitRunnable


class Battleroyal2 : JavaPlugin() {
    var isStarted: Boolean = false
    private lateinit var scoreboard: Scoreboard
    lateinit var subTeam: Team
    lateinit var memTeam: Team
    lateinit var bossBar: BossBar

    override fun onEnable() {
        // 리스너 등록
        server.pluginManager.registerEvents(BattleroyalListener(this), this)

        // 커맨드 등록
        getCommand("battleroyal")?.setExecutor(CommandHandler(this))
        getCommand("battleroyal")?.tabCompleter = BattleroyalTabCompleter()
        getCommand("changenick")?.setExecutor(CommandHandler(this))
        getCommand("changenick")?.tabCompleter = BattleroyalTabCompleter()
        getCommand("makeSpawn")?.setExecutor(CommandHandler(this))
        getCommand("addteam")?.setExecutor(CommandHandler(this))
        getCommand("addteam")?.tabCompleter = BattleroyalTabCompleter()

        scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        subTeam = scoreboard.getTeam("subTeam") ?: scoreboard.registerNewTeam("subTeam")
        memTeam = scoreboard.getTeam("memTeam") ?: scoreboard.registerNewTeam("memTeam")


        subTeam.color(NamedTextColor.RED)
        memTeam.color(NamedTextColor.BLUE)


        // 설정값 불러오기
        isStarted = config.getBoolean("isStarted", false)
        for (player in server.onlinePlayers) {
            val nickname = config.getString("players.${player.uniqueId}.nickname")
            if (nickname != null) {
                changePlayerName(player, nickname)
            }
        }

        if (!isStarted) {
            setup()
            //makeSpawn()
        }

        logger.info("플러그인 로드 완료")
    }

    override fun onDisable() {
        getCommand("battleroyal")?.setExecutor(null)
        getCommand("changenick")?.setExecutor(null)
        getCommand("addTeam")?.setExecutor(null)
        getCommand("makeSpawn")?.setExecutor(null)
    }

    fun setup() {
        val world = server.getWorld("world") ?: return
        world.difficulty = org.bukkit.Difficulty.HARD
        world.setSpawnLocation(org.bukkit.Location(world, 0.0, 201.0, 0.0))
        world.worldBorder.center = org.bukkit.Location(world, 0.0, 0.0, 0.0)
    }

    fun makeSpawn() {
        val world = server.getWorld("world") ?: return
        val baseX = -15
        val baseY = 200
        val baseZ = -15
        for (x in 0..30) {
            for (z in 0..30) {
                world.getBlockAt(baseX + x, baseY, baseZ + z).type = org.bukkit.Material.GRASS_BLOCK
                world.getBlockAt(baseX + x, baseY + 15, baseZ + z).type = org.bukkit.Material.LIGHT_GRAY_STAINED_GLASS
            }
        }
        for (i in 0..30) {
            for (j in 0..13) {
                world.getBlockAt(baseX + i, baseY + 1 + j, baseZ - 1).type = org.bukkit.Material.LIGHT_GRAY_STAINED_GLASS
                world.getBlockAt(baseX + i, baseY + 1 + j, baseZ + 31).type = org.bukkit.Material.LIGHT_GRAY_STAINED_GLASS
                world.getBlockAt(baseX - 1, baseY + 1 + j, baseZ + i).type = org.bukkit.Material.LIGHT_GRAY_STAINED_GLASS
                world.getBlockAt(baseX + 31, baseY + 1 + j, baseZ + i).type = org.bukkit.Material.LIGHT_GRAY_STAINED_GLASS
            }
        }
    }

    fun changePlayerName(player: Player, nickname: String) {

        if (nickname == "reset" || nickname == player.name) {
            player.playerListName(null)

            config.set("players.${player.uniqueId}.nickname", null)
            saveConfig()

        } else {
            val team: Team? = scoreboard.getEntryTeam(player.name)

            player.playerListName(Component.text(nickname,team?.color() ?: NamedTextColor.WHITE))
            config.set("players.${player.uniqueId}.nickname", nickname)
            saveConfig()
        }

    }

    fun addSubTeam(sender: Player, targetName: String, team: String) {
        val targetPlayer = Bukkit.getPlayerExact(targetName)
        val team = scoreboard.getTeam(team) ?: return

        if (targetPlayer == null) {
            sender.sendMessage("해당 닉네임의 플레이어가 온라인이 아닙니다.")
            return
        } else {
            team.addPlayer(targetPlayer)
            sender.sendMessage("${targetPlayer.name}님을 ${team.name} 팀에 추가했습니다.")

            if (targetPlayer.playerListName() != null) {
                changePlayerName(targetPlayer, config.getString("players.${targetPlayer.uniqueId}.nickname") ?: targetPlayer.name)

            }
        }
    }


    fun pickaxeTier(material: Material): Int = when (material) {
        Material.WOODEN_PICKAXE -> 1
        Material.STONE_PICKAXE -> 2
        Material.IRON_PICKAXE -> 3
        Material.GOLDEN_PICKAXE -> 2
        Material.DIAMOND_PICKAXE -> 4
        Material.NETHERITE_PICKAXE -> 5
        else -> 0
    }


    fun getSafeTeamBase(centerX: Int, centerZ: Int, size: Int, world: World, blockedBlocks: Collection<Material>): Pair<Int, Int> {
        var baseX: Int
        var baseZ: Int
        var attempts = 0
        while (true) {
            baseX = centerX + (-size / 2..size / 2).random()
            baseZ = centerZ + (-size / 2..size / 2).random()
            val baseY = world.getHighestBlockYAt(baseX, baseZ)
            val blockBelow = world.getBlockAt(baseX, baseY, baseZ)
            if (blockBelow.type !in blockedBlocks) {
                break
            }
            attempts++
            if (attempts > 50) break
        }
        return baseX to baseZ
    }

    fun teleportTeam(team: Team, baseX: Int, baseZ: Int, world: World, blockedBlocks: Collection<Material>) {
        for (entry in team.entries) {
            val player = Bukkit.getPlayer(entry) ?: continue
            while (true) {
                val x = baseX + (-2..2).random()
                val z = baseZ + (-2..2).random()
                val y = world.getHighestBlockYAt(x, z) + 1
                val blockBelow = world.getBlockAt(x, y - 1, z)
                if (blockBelow.type !in blockedBlocks) {
                    player.teleport(Location(world, x.toDouble(), y.toDouble(), z.toDouble()))
                    break
                }
            }

        }
    }

    fun startCountdown(seconds: Int, bossBar: BossBar) {
        var remaining = seconds
        bossBar.progress = 1.0 // 게이지 초기화[3]

        object : BukkitRunnable() {
            override fun run() {
                if (remaining > 0) {

                    bossBar.setTitle("남은 시간: ${remaining}초") // 텍스트 업데이트[3]
                    bossBar.progress = remaining.toDouble() / seconds.toDouble() // 게이지 계산[3]
                    remaining--
                } else {
                    cancel() // 타이머 종료[3]
                }
            }
        }.runTaskTimer(this, 0, 20) // 1초 간격 실행[3]
    }

    fun startGame(sender : CommandSender = server.consoleSender, worldSize : Int = 3000) {
        val world = server.getWorld("world") ?: return

        isStarted = !isStarted
        config.set("isStarted", isStarted)
        saveConfig()

        val baseX = -16
        val baseY = 200
        val baseZ = -16
        for (x in 0 until 33) {
            for (y in 0 until 16) {
                for (z in 0 until 33) {
                    world.getBlockAt(baseX + x, baseY + y, baseZ + z).type = org.bukkit.Material.AIR

                }
            }
        }

        world.time = 0
        world.clearWeatherDuration = 20 * 60 * 10
        world.weatherDuration = 20 * 60 * 10
        world.thunderDuration = 0
        world.isThundering = false

        bossBar = Bukkit.createBossBar("Time left", BarColor.RED, BarStyle.SOLID)
        bossBar.isVisible = true

        for (player in server.onlinePlayers) {
            player.gameMode = GameMode.SURVIVAL
            player.inventory.clear()
            player.inventory.addItem(ItemStack(Material.COOKED_BEEF, 32))
            player.showTitle(Title.title(
                Component.text("배틀로얄이 시작되었습니다!"),
                Component.text("생존을 위해 싸우세요!")
            ))

            bossBar.addPlayer(player)
        }

        world.worldBorder.size = worldSize.toDouble()


        val blockedBlocks = listOf(Material.WATER, Material.LAVA, Material.BAMBOO)

        val (subX, subZ) = getSafeTeamBase(0, 0, worldSize, world, blockedBlocks)
        var memX: Int
        var memZ: Int
        do {
            val (x, z) = getSafeTeamBase(0, 0, worldSize, world, blockedBlocks)
            memX = x
            memZ = z
            val distance = Math.sqrt(((subX - memX).toDouble().pow(2) + (subZ - memZ).toDouble().pow(2)))
        } while (distance < 1000)

        teleportTeam(subTeam, subX, subZ, world, blockedBlocks)
        teleportTeam(memTeam, memX, memZ, world, blockedBlocks)

        startCountdown(300, bossBar)


        sender.sendMessage("배틀로얄이 시작되었습니다! (isStarted = ${isStarted})")
    }
}
