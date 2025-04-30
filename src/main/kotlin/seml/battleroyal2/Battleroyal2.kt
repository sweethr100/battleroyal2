package seml.battleroyal2

import org.bukkit.plugin.java.JavaPlugin

class Battleroyal2 : JavaPlugin() {
    var isStarted: Boolean = false

    override fun onEnable() {
        // 리스너 등록
        server.pluginManager.registerEvents(BattleroyalListener(this), this)
        // 커맨드 등록
        getCommand("battleroyal")?.setExecutor(CommandHandler(this))
        getCommand("changenick")?.setExecutor(CommandHandler(this))

        // 설정값 불러오기
        isStarted = config.getBoolean("isStarted", false)

        if (!isStarted) {
            setup()
            makeSpawn()
        }

        logger.info("플러그인 로드 완료")
    }

    override fun onDisable() {
        getCommand("battleroyal")?.setExecutor(null)
        getCommand("changenick")?.setExecutor(null)
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

    fun pickaxeTier(material: org.bukkit.Material): Int = when (material) {
        org.bukkit.Material.WOODEN_PICKAXE -> 1
        org.bukkit.Material.STONE_PICKAXE -> 2
        org.bukkit.Material.IRON_PICKAXE -> 3
        org.bukkit.Material.GOLDEN_PICKAXE -> 2
        org.bukkit.Material.DIAMOND_PICKAXE -> 4
        org.bukkit.Material.NETHERITE_PICKAXE -> 5
        else -> 0
    }
}
