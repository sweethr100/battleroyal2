package seml.battleroyal2

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import com.destroystokyo.paper.profile.PlayerProfile
import com.sun.tools.javac.tree.TreeInfo.args
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player

class CommandHandler(val plugin: Battleroyal2) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (command.name.equals("battleroyal", ignoreCase = true) && sender.isOp) {
            if (args.isEmpty()) {
                sender.sendMessage("사용법: /help battleroyal")
                return true
            }

            when (args[0].lowercase()) {
                "start" -> {
                    plugin.startGame(sender, 3000)
                }
            }
            return true
        }
        else if (command.name.equals("makeSpawn", ignoreCase = true) && sender.isOp) {
            if (sender is Player && sender.isOp) {
                plugin.makeSpawn()
                sender.sendMessage("대기실이 생성되었습니다.")
                return true
            }
        }

        else if (command.name.equals("addTeam", ignoreCase = true) && sender.isOp) {
            if (sender is Player && sender.isOp) {
                if (args.size < 2) {
                    sender.sendMessage("사용법: /help addTeam")
                    return true
                } else {
                    plugin.addTeam(sender, args[1], args[0])
                    return true
                }
            }
        }



        else if (command.name.equals("changenick", ignoreCase = true)) {
            if (sender is Player && sender.isOp) {
                if (args.isNotEmpty()) {
                    plugin.changePlayerName(sender, args[0])
                    sender.sendMessage("닉네임이 변경되었습니다.")
                }

            } else {
                sender.sendMessage("사용할 수 없는 명령어입니다.")
            }
            return true
        }
        return false
    }
}
