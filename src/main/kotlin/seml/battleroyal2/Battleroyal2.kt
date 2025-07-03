package seml.battleroyal2

import BattleroyalTabCompleter
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.world
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.Location
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import kotlin.math.pow
import net.kyori.adventure.title.Title
import org.bukkit.GameRule
import org.bukkit.SoundCategory
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.Sound


class Battleroyal2 : JavaPlugin() {
    var isStarted: Boolean = false
    lateinit var scoreboard: Scoreboard
    lateinit var subTeam: Team
    lateinit var memTeam: Team
    val bossBars: MutableList<BossBar> = mutableListOf()

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

        subTeam.displayName(Component.text("구독자팀"))
        memTeam.displayName(Component.text("삼극팀"))

        subTeam.setAllowFriendlyFire(false)
        memTeam.setAllowFriendlyFire(false)


        if (!isStarted) {
            setup()
            makeSpawn()
        }

        logger.info("플러그인 로드 완료")

        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            updatePlayerCountInSideBar()
        }, 0L, 20L) // 0L: 바로 시작, 20L: 반복 간격(틱 단위, 1초)

    }

    override fun onDisable() {
        getCommand("battleroyal")?.setExecutor(null)
        getCommand("changenick")?.setExecutor(null)
        getCommand("addTeam")?.setExecutor(null)
        getCommand("makeSpawn")?.setExecutor(null)

        for (bossBar in bossBars) {
            bossBar.removeAll()
        }
    }

    fun setup() {
        val world = server.getWorld("world") ?: return
        world.difficulty = org.bukkit.Difficulty.HARD
        world.setSpawnLocation(Location(world, 0.0, 201.0, 0.0))
        world.worldBorder.center = Location(world, 0.0, 0.0, 0.0)

        world.setGameRule(GameRule.LOCATOR_BAR, false)
    }

    fun makeSpawn() {
        val world = server.getWorld("world") ?: return
        val baseX = -15
        val baseY = 200
        val baseZ = -15
        for (x in 0..30) {
            for (z in 0..30) {
                world.getBlockAt(baseX + x, baseY, baseZ + z).type = Material.GRASS_BLOCK
                world.getBlockAt(baseX + x, baseY + 15, baseZ + z).type = Material.LIGHT_GRAY_STAINED_GLASS
            }
        }
        for (i in 0..30) {
            for (j in 0..13) {
                world.getBlockAt(baseX + i, baseY + 1 + j, baseZ - 1).type = Material.LIGHT_GRAY_STAINED_GLASS
                world.getBlockAt(baseX + i, baseY + 1 + j, baseZ + 31).type = Material.LIGHT_GRAY_STAINED_GLASS
                world.getBlockAt(baseX - 1, baseY + 1 + j, baseZ + i).type = Material.LIGHT_GRAY_STAINED_GLASS
                world.getBlockAt(baseX + 31, baseY + 1 + j, baseZ + i).type = Material.LIGHT_GRAY_STAINED_GLASS
            }
        }
    }

    fun changePlayerName(player: Player, nickname: String?) {
        val team: Team? = scoreboard.getEntryTeam(player.name)

        if (nickname == "reset" || nickname == null) {
            player.playerListName(Component.text(player.name, team?.color() ?: NamedTextColor.WHITE))
            config.set("players.${player.uniqueId}.nickname", null)
            saveConfig()
            reloadConfig()

        } else {
            player.playerListName(Component.text(nickname,team?.color() ?: NamedTextColor.WHITE))
            config.set("players.${player.uniqueId}.nickname", nickname)
            saveConfig()
            reloadConfig()
        }

    }

    fun addTeam(sender: Player, targetName: String, team: String) {
        val targetPlayer = Bukkit.getPlayerExact(targetName)
        val team = scoreboard.getTeam(team) ?: return

        if (targetPlayer == null) {
            sender.sendMessage("해당 닉네임의 플레이어가 온라인이 아닙니다.")
            return
        } else {
            val playerName = PlainTextComponentSerializer.plainText().serialize(targetPlayer.playerListName()) ?: null

            team.addEntry(targetPlayer.name)
            changePlayerName(targetPlayer, playerName)
            sender.sendMessage("${targetPlayer.name}님을 ${team.name} 팀에 추가했습니다.")

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

    fun startCountdown(seconds: Int, title: String, onFinish: () -> Unit = {}) {
        var remaining = seconds
        val bossBar = Bukkit.createBossBar("자기장 축소까지", BarColor.WHITE, BarStyle.SOLID)
        bossBars.add(bossBar)
        bossBar.isVisible = true
        bossBar.progress = 1.0

        Bukkit.getOnlinePlayers().forEach { bossBar.addPlayer(it) }

        object : BukkitRunnable() {
            override fun run() {
                if (remaining > 0) {
                    val minutes = remaining / 60
                    val secs = remaining % 60
                    val timeString = String.format("%02d:%02d", minutes, secs)

                    if (remaining <= 30) {
                        bossBar.color = BarColor.RED
                    } else {
                        bossBar.color = BarColor.WHITE
                    }

                    bossBar.setTitle("$title : $timeString") // 텍스트 업데이트[3]
                    bossBar.progress = remaining.toDouble() / seconds.toDouble() // 게이지 계산[3]
                    remaining--
                } else {
                    bossBar.removeAll()
                    cancel() // 타이머 종료[3]
                    onFinish()
                }
            }
        }.runTaskTimer(this, 0, 20) // 1초 간격 실행[3]
    }


    fun runWorldBorderEventsSequentially(
        plugin: JavaPlugin,
        world: World,
        eventTimes: List<Triple<Int, Int, Int>>,
        index: Int = 0,
    ) {
        if (index >= eventTimes.size) {
            return
        }

        val (time, size, duration) = eventTimes[index]
        startCountdown(time, "자기장 축소까지") {

            world.worldBorder.setSize(size.toDouble(), duration.toLong())

            for (player in Bukkit.getOnlinePlayers()) {
                player.sendMessage("자기장이 축소됩니다!")
                // 알림음 재생 (예: LEVEL_UP)
                player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
            }

            // duration초(마인크래프트 틱 단위로 환산) 후 다음 이벤트 실행
            startCountdown(duration, "자기장 축소중") {
                runWorldBorderEventsSequentially(plugin, world, eventTimes, index + 1)
            }
        }
    }

    fun updatePlayerCountInSideBar() {

        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val objective = scoreboard.registerNewObjective(
            "PlayerCount", // 오브젝티브 이름
            Criteria.DUMMY,  // 기준 (기존 "dummy" 대신 Enum 사용)
            Component.text("정보") // 표시 이름 (Component로 래핑)
        )
        val memCount = Bukkit.getOnlinePlayers().count { Bukkit.getScoreboardManager().mainScoreboard.getEntryTeam(it.name) == memTeam && it.gameMode == GameMode.SURVIVAL }
        val subCount = Bukkit.getOnlinePlayers().count { Bukkit.getScoreboardManager().mainScoreboard.getEntryTeam(it.name) == subTeam && it.gameMode == GameMode.SURVIVAL }

        objective.displaySlot = DisplaySlot.SIDEBAR
        objective.getScore("삼극팀 인원").score = memCount
        objective.getScore("구독자팀 인원").score = subCount

        val worldbordersize = server.worlds[0].worldBorder.size.toInt()
        objective.getScore("자기장크기").score = worldbordersize

        Bukkit.getOnlinePlayers().forEach { it.scoreboard = scoreboard }
    }


    fun startGame(sender : CommandSender = server.consoleSender, worldSize : Int = 3000) {
        val world = server.getWorld("world") ?: return

        isStarted = true

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

        subTeam.setAllowFriendlyFire(false)
        memTeam.setAllowFriendlyFire(false)

        world.time = 0
        world.setStorm(false)
        world.setThundering(false)


        world.worldBorder.size = worldSize.toDouble()

        val blockedBlocks = listOf(Material.WATER, Material.LAVA, Material.BAMBOO, Material.KELP)

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

        for (player in server.onlinePlayers) {
            player.gameMode = GameMode.SURVIVAL
            player.inventory.clear()
            player.inventory.addItem(ItemStack(Material.COOKED_BEEF, 32))
            player.showTitle(Title.title(
                Component.text("배틀로얄이 시작되었습니다!"),
                Component.text("생존을 위해 싸우세요!")
            ))

            player.playSound(player.location, Sound.BLOCK_BELL_USE, 1.0f, 0.5f)
        }

        updatePlayerCountInSideBar()

        val eventTimes = listOf(
            Triple(1080,2000,600),
            Triple(900,1000,600),
            Triple(720,500,600),
            Triple(600,1,300)
        )

        runWorldBorderEventsSequentially(this, world, eventTimes)


        sender.sendMessage("배틀로얄이 시작되었습니다!")
    }

    fun endGame() {
        val survivors = Bukkit.getOnlinePlayers().filter { it.gameMode == GameMode.SURVIVAL }

        for (player in Bukkit.getOnlinePlayers()) {
            val displayName: Component = scoreboard.getEntryTeam(survivors[0].name)?.displayName() ?: Component.text("error")
            val plainName: String = PlainTextComponentSerializer.plainText().serialize(displayName)

            player.showTitle(Title.title(
                Component.text("게임 종료!"),
                Component.text("#1 : ${plainName}", NamedTextColor.GOLD),
            ))

            player.playSound(
                player.location, // 플레이어 위치에서
                Sound.ITEM_TRIDENT_THUNDER, // 소리 이름 (Enum)
                SoundCategory.MASTER, // 소리 카테고리 (MASTER, PLAYER, AMBIENT 등)
                1.0f, // 볼륨
                1.0f  // 피치
            )

            player.playSound(
                player.location, // 플레이어 위치에서
                Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, // 소리 이름 (Enum)
                SoundCategory.MASTER, // 소리 카테고리 (MASTER, PLAYER, AMBIENT 등)
                1.0f, // 볼륨
                1.0f  // 피치
            )
        }
    }
}
