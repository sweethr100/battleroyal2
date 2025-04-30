package seml.battleroyal2

import io.papermc.paper.command.brigadier.argument.ArgumentTypes.world
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

        if (command.name.equals("battleroyal", ignoreCase = true) && sender.isOp) {
            if (args[0] == "start") {
                plugin.startGame(sender)
            }
           else if (args[0] == "makeSpawn") {
               plugin.makeSpawn()
            }
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
                }

            } else {
                sender.sendMessage("사용할 수 없는 명령어입니다.")
            }
            return true
        }
        return false
    }
}
