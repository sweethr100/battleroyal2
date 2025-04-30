package seml.battleroyal2

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component

class CommandHandler(val plugin: Battleroyal2) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val world = plugin.server.getWorld("world") ?: return false

        if (command.name.equals("battleroyal", ignoreCase = true) && sender.isOp) {
            plugin.isStarted = !plugin.isStarted
            plugin.config.set("isStarted", plugin.isStarted)
            plugin.saveConfig()

            val baseX = -16
            val baseY = 200
            val baseZ = -16
            for (x in 0 until 32) {
                for (y in 0 until 16) {
                    for (z in 0 until 32) {
                        world.getBlockAt(baseX + x, baseY + y, baseZ + z).type = org.bukkit.Material.AIR
                    }
                }
            }

            world.time = 0
            world.clearWeatherDuration = 20 * 60 * 10
            world.weatherDuration = 20 * 60 * 10
            world.thunderDuration = 0
            world.isThundering = false

            sender.sendMessage("배틀로얄이 시작되었습니다! (isStarted = ${plugin.isStarted})")
            return true
        }

        if (command.name.equals("changenick", ignoreCase = true)) {
            if (sender is Player && sender.isOp) {
                if (args.isNotEmpty()) {
                    val newNick = if (args[0] == "reset") sender.name else args[0]
                    if (args[0] == "reset") {
                        sender.sendMessage("닉네임이 초기화되었습니다.")
                    } else {
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
}
