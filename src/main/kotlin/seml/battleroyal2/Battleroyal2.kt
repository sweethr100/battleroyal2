package seml.battleroyal2


import io.papermc.paper.command.brigadier.argument.ArgumentTypes.gameMode
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.world
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.World
import org.bukkit.Material
import org.bukkit.Location
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.Difficulty
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.entity.Monster
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.entity.Player
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.ExperienceOrb
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.EntityDamageEvent


class Battleroyal2 : JavaPlugin(), Listener {

    var isStarted: Boolean = false

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)

        isStarted = config.getBoolean("isStarted", false)


        if (isStarted == false) {
            setup()
            makeSpawn()
        }

    }

    override fun onDisable() {

    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val world = server.getWorld("world") ?: return false
        if (command.name.equals("battleroyal", ignoreCase = true) && sender.isOp) {


            isStarted = !isStarted
            config.set("isStarted", isStarted)
            saveConfig()

            val baseX = -16
            val baseY = 200
            val baseZ = -16
            for (x in 0 until 32) {
                for (y in 0 until 16) {
                    for (z in 0 until 32) {
                        world.getBlockAt(baseX + x, baseY + y, baseZ + z).type = Material.AIR
                    }
                }
            }

            world.time = 0
            world.clearWeatherDuration = 20 * 60 * 10 // 10분간 맑음 (틱 단위)
            world.weatherDuration = 20 * 60 * 10
            world.thunderDuration = 0
            world.isThundering = false

            sender.sendMessage("배틀로얄이 시작되었습니다! (isStarted = $isStarted)")
            return true
        }
        if (command.name.equals("changenick", ignoreCase = true)) {
            if (sender is Player && sender.isOp) {
                if (args.isNotEmpty()) {
                    var newNick: String
                    if (args[0] == "reset") {
                        newNick = sender.name // "/changenick 세믈" 입력 시 newNick == "세믈"

                        sender.sendMessage("닉네임이 초기화되었습니다.")
                    } else {
                         newNick = args[0] // "/changenick 세믈" 입력 시 newNick == "세믈"

                        sender.sendMessage("닉네임이 '$newNick'(으)로 변경되었습니다.")
                    }

                    sender.displayName(Component.text(newNick))
                    sender.playerListName(Component.text(newNick))
                    sender.customName(Component.text(newNick))

                } else {
                    sender.sendMessage("변경할 닉네임을 입력하세요: /changenick <닉네임 | reset>")
                }
            } else {
                sender.sendMessage("사용할 수 없는 명령어입니다.")
            }
            return true
        }
        return false
    }

    private fun setup() {
        val world = server.getWorld("world") ?: return
        world?.difficulty = Difficulty.HARD
        world.setSpawnLocation(Location(world, 0.0, 201.0, 0.0))

        // 게임룰 바꾸기
        world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.setGameRule(org.bukkit.GameRule.FALL_DAMAGE, false)
        world.worldBorder.center = Location(world, 0.0, 0.0, 0.0)
    }

    private fun makeSpawn() {
        val world: World = server.getWorld("world") ?: return
        val baseX = -15
        val baseY = 200
        val baseZ = -15

        for (x in 0 until 30) {
            for (z in 0 until 30) {
                world.getBlockAt(baseX + x, baseY, baseZ + z).type = Material.GRASS_BLOCK
                world.getBlockAt(baseX + x, baseY + 15, baseZ + z).type = Material.GLASS
            }
        }

        for (i in 0 until 30) {
            for (j in 0 until 14) {
                world.getBlockAt(baseX + i, baseY + 1 + j, baseZ - 1).type = Material.GLASS
                world.getBlockAt(baseX + i, baseY + 1 + j, baseZ + 30).type = Material.GLASS
                world.getBlockAt(baseX - 1, baseY + 1 + j, baseZ + i).type = Material.GLASS
                world.getBlockAt(baseX + 30, baseY + 1 + j, baseZ + i).type = Material.GLASS
            }
        }
        logger.info("대기실 생성 완료!")
    }

    fun pickaxeTier(material: Material): Int = when (material) {
        Material.WOODEN_PICKAXE -> 1
        Material.STONE_PICKAXE -> 2
        Material.IRON_PICKAXE -> 3
        Material.GOLDEN_PICKAXE -> 2 // 금곡괭이는 돌과 같은 등급
        Material.DIAMOND_PICKAXE -> 4
        Material.NETHERITE_PICKAXE -> 5
        else -> 0
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!event.player.hasPlayedBefore()) {
            val world = Bukkit.getWorld("world") ?: return
            val spawnLoc = Location(world, 0.5, 201.0, 0.5) // 블록 중앙
            event.player.teleport(spawnLoc)

            if (isStarted == false) {
                event.player.sendMessage("대기실로 이동되었습니다!")
                event.player.gameMode = GameMode.ADVENTURE
            }
            else {
                event.player.sendMessage("관전자로 변경되었습니다!")
                event.player.gameMode = GameMode.SPECTATOR
            }
        }
    }
    @EventHandler
    fun onAsyncChat(event: AsyncChatEvent) {
        event.isCancelled = true
        event.player.sendMessage("채팅이 비활성화되어 있습니다.")
    }
    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        // 몬스터(적대적 몹)만 차단하고 싶다면:
        if (event.entityType.isAlive && event.entityType.isSpawnable && event.entity is Monster) {
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onPlayerPortal(event: PlayerPortalEvent) {
        // 네더 또는 엔드 포탈 사용 시 차단
        event.isCancelled = true
        event.player.sendMessage("이 서버에서는 네더와 엔더로 이동할 수 없습니다!")
    }
    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val victim = event.entity
        // 플레이어가 어떤 종류의 데미지든 받는 경우 차단
        if (victim is Player && isStarted == false) {
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val blockState = event.blockState
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (player.gameMode != GameMode.CREATIVE) {
            when (blockState.type) {
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> {
                    if (pickaxeTier(item.type) >= 2) {
                        // 기존 드롭 제거
                        event.getItems().clear()
                        // 주괴 드롭 (ItemStack → Item으로 변환)
                        val item = event.block.world.dropItem(event.block.location.add(0.5,0.5,0.5), ItemStack(Material.IRON_INGOT))
                        event.getItems().add(item)

                        val orb = event.block.world.spawn(event.block.location.add(0.5, 0.5, 0.5), ExperienceOrb::class.java)
                        orb.experience = 3 // 철광석은 보통 1-3 경험치

                    }
                }
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> {
                    if (pickaxeTier(item.type) >= 3) {
                        event.getItems().clear()
                        val item = event.block.world.dropItem(event.block.location.add(0.5,0.5,0.5), ItemStack(Material.GOLD_INGOT))
                        event.getItems().add(item)

                        val orb = event.block.world.spawn(event.block.location.add(0.5, 0.5, 0.5), ExperienceOrb::class.java)
                        orb.experience = 5 // 금광석은 보통 더 많은 경험치
                    }
                }
                Material.RAW_IRON_BLOCK -> {
                    if (pickaxeTier(item.type) >= 2) {
                        event.getItems().clear()
                        val item = event.block.world.dropItem(event.block.location.add(0.5,0.5,0.5), ItemStack(Material.IRON_BLOCK))
                        event.getItems().add(item)

                        val orb = event.block.world.spawn(event.block.location.add(0.5, 0.5, 0.5), ExperienceOrb::class.java)
                        orb.experience = 9 // 철 원석 블록은 더 많은 경험치
                    }
                }
                Material.RAW_GOLD_BLOCK -> {
                    if (pickaxeTier(item.type) >= 3) {
                        event.getItems().clear()
                        val item = event.block.world.dropItem(event.block.location.add(0.5,0.5,0.5), ItemStack(Material.GOLD_BLOCK))
                        event.getItems().add(item)

                        val orb = event.block.world.spawn(event.block.location.add(0.5, 0.5, 0.5), ExperienceOrb::class.java)
                        orb.experience = 15 // 금 원석 블록은 더 많은 경험치
                    }
                }

                else -> {}
            }
        }
    }
    @EventHandler
    fun onCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val blocked = listOf("msg", "tell", "w")
        val command = event.message.split(" ")[0].removePrefix("/").lowercase()
        if (command in blocked) {
            event.isCancelled = true
            event.player.sendMessage("귓속말 명령어는 사용할 수 없습니다.")
        }
    }
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val playerName = if (player.displayName != null) {
            player.displayName
        } else {
            player.name
        }
        event.deathMessage(Component.text("§c${playerName}이(가) 죽었습니다"))

        if (isStarted) player.gameMode = GameMode.SPECTATOR
    }
}
