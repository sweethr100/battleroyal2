package seml.battleroyal2

import BattleroyalTabCompleter
import com.sun.tools.javac.tree.TreeInfo.args
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
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


class Battleroyal2 : JavaPlugin() {
    var isStarted: Boolean = false
    private lateinit var scoreboard: Scoreboard
    lateinit var subTeam: Team
    lateinit var memTeam: Team

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


    fun pickaxeTier(material: org.bukkit.Material): Int = when (material) {
        org.bukkit.Material.WOODEN_PICKAXE -> 1
        org.bukkit.Material.STONE_PICKAXE -> 2
        org.bukkit.Material.IRON_PICKAXE -> 3
        org.bukkit.Material.GOLDEN_PICKAXE -> 2
        org.bukkit.Material.DIAMOND_PICKAXE -> 4
        org.bukkit.Material.NETHERITE_PICKAXE -> 5
        else -> 0
    }


    fun spreadTeam(team: Team, centerX: Int, centerZ: Int, size: Int, world: World) {
        val baseX = centerX + (-size/2..size/2).random()
        val baseZ = centerZ + (-size/2..size/2).random()
        for (entry in team.entries) {
            val player = Bukkit.getPlayer(entry) ?: continue

            val x = baseX + (-2..2).random() // -2, -1, 0, 1, 2
            val z = baseZ + (-2..2).random() // -2, -1, 0, 1, 2
            val y = world.getHighestBlockYAt(x, z) + 1
            player.teleport(Location(world, x.toDouble(), y.toDouble(), z.toDouble()))
        }
    }

    fun startGame(sender : CommandSender = server.consoleSender) {
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

        for (player in server.onlinePlayers) {
            player.gameMode = GameMode.SURVIVAL
            player.inventory.clear()
            player.inventory.addItem(ItemStack(Material.COOKED_BEEF, 32))
        }

        // 사용 예시
        val players = server.onlinePlayers.shuffled().toList()
        spreadTeam(subTeam, 0, 0, 3000, world)
        //spreadTeam(teamB, 0, 0, 3000, world)

        sender.sendMessage("배틀로얄이 시작되었습니다! (isStarted = ${isStarted})")
    }
}
