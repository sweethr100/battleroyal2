package seml.battleroyal2

import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import kotlin.random.Random
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.event.weather.WeatherChangeEvent

class BattleroyalListener(val plugin: Battleroyal2) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val nickname = plugin.config.getString("players.${event.player.uniqueId}.nickname")
        if (nickname != null) {
            plugin.changePlayerName(event.player, nickname)
        }

        if (!event.player.hasPlayedBefore()) {
            val world = Bukkit.getWorld("world") ?: return
            val spawnLoc = Location(world, 0.5, 201.0, 0.5)
            event.player.teleport(spawnLoc)
            if (!plugin.isStarted) {
                event.player.sendMessage("대기실로 이동되었습니다!")
                event.player.gameMode = GameMode.ADVENTURE
            } else {
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
        if (
            (event.entityType.isAlive && event.entityType.isSpawnable && event.entity is Monster)
            || event.entityType == EntityType.PHANTOM
            ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerPortal(event: PlayerPortalEvent) {
        event.isCancelled = true
        event.player.sendMessage("이 서버에서는 네더와 엔더로 이동할 수 없습니다!")
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val victim = event.entity
        if (victim is Player && !plugin.isStarted) {
            event.isCancelled = true
        } else if (victim is Player && event.cause == EntityDamageEvent.DamageCause.LAVA) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {

        val victim = event.entity
        val damager = event.damager

        if (victim !is Player || damager !is Player) return

        val damagerTeam = plugin.scoreboard.getEntryTeam(damager.name)
        val victimTeam = plugin.scoreboard.getEntryTeam(victim.name)

        // 같은 팀 소속 확인
        if (damagerTeam != null && damagerTeam == victimTeam) {
            event.isCancelled = true
            damager.sendActionBar(Component.text("아군을 공격할 수 없습니다!", NamedTextColor.RED))
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        // isStarted가 false면 배고픔이 줄지 않게 막음
        if (!plugin.isStarted) {
            event.isCancelled = true
        }
    }


    @EventHandler
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val blockState = event.blockState
        val player = event.player
        val item = player.inventory.itemInMainHand
        val block = event.block
        if (player.gameMode != GameMode.CREATIVE) {
            when (blockState.type) {
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> {
                    if (plugin.pickaxeTier(item.type) >= 2) {
                        event.items.clear()
                        val itemEntity = block.world.dropItem(block.location.add(0.5,0.5,0.5), ItemStack(Material.IRON_INGOT))
                        event.items.add(itemEntity)
                        val orb = block.world.spawn(block.location.add(0.5, 0.5, 0.5), ExperienceOrb::class.java)
                        orb.experience = 3
                    }
                }
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> {
                    if (plugin.pickaxeTier(item.type) >= 3) {
                        event.items.clear()
                        val itemEntity = block.world.dropItem(block.location.add(0.5,0.5,0.5), ItemStack(Material.GOLD_INGOT))
                        event.items.add(itemEntity)
                        val orb = block.world.spawn(block.location.add(0.5, 0.5, 0.5), ExperienceOrb::class.java)
                        orb.experience = 5
                    }
                }
                Material.RAW_IRON_BLOCK -> {
                    if (plugin.pickaxeTier(item.type) >= 2) {
                        event.items.clear()
                        val itemEntity = block.world.dropItem(block.location.add(0.5,0.5,0.5), ItemStack(Material.IRON_BLOCK))
                        event.items.add(itemEntity)
                        val orb = block.world.spawn(block.location.add(0.5, 0.5, 0.5), ExperienceOrb::class.java)
                        orb.experience = 9
                    }
                }
                Material.RAW_GOLD_BLOCK -> {
                    if (plugin.pickaxeTier(item.type) >= 3) {
                        event.items.clear()
                        val itemEntity = block.world.dropItem(block.location.add(0.5,0.5,0.5), ItemStack(Material.GOLD_BLOCK))
                        event.items.add(itemEntity)
                        val orb = block.world.spawn(block.location.add(0.5, 0.5, 0.5), ExperienceOrb::class.java)
                        orb.experience = 15
                    }
                }
                else -> {}
            }
            if (blockState.type.name.endsWith("_LEAVES")) {
                if (Random.nextDouble() < 0.20) {
                    block.world.dropItemNaturally(block.location, ItemStack(Material.APPLE))
                }
            }
        }
    }

    @EventHandler
    fun onCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val blocked = listOf("msg", "tell", "w", "me","teammsg")
        val command = event.message.split(" ")[0].removePrefix("/").lowercase()
        if (command in blocked) {
            event.isCancelled = true
            event.player.sendMessage("이 명령어는 사용할 수 없습니다.")
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val playerName = PlainTextComponentSerializer.plainText().serialize(player.playerListName()) ?: player.name
        event.deathMessage(Component.text("§c${playerName}이(가) 죽었습니다"))
        if (plugin.isStarted) player.gameMode = GameMode.SPECTATOR

        val survivalCount = Bukkit.getOnlinePlayers().count { it.gameMode == GameMode.SURVIVAL }
        player.sendMessage(Component.text("당신의 순위 : ${survivalCount + 1}위").decorate(TextDecoration.BOLD))

        if (survivalCount == 1) {
            plugin.endGame()
        }

        plugin.updatePlayerCountInSideBar()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.updatePlayerCountInSideBar()

        val survivalCount = Bukkit.getOnlinePlayers().count { it.gameMode == GameMode.SURVIVAL }

        if (survivalCount == 1) {
            plugin.endGame()
        }

        plugin.updatePlayerCountInSideBar()
    }

    @EventHandler
    fun onWeatherChange(event: WeatherChangeEvent) {
        // 날씨가 비/눈 등으로 바뀌려 할 때
        if (!plugin.isStarted && event.toWeatherState()) {
            event.isCancelled = true
        }
    }

}
