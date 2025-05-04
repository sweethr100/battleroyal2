import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class BattleroyalTabCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        // /battleroyal <여기서 탭>
        if (command.name.equals("battleroyal", ignoreCase = true)) {
            if (args.size == 1) {
                // 첫 번째 인자에서 탭 누르면 이 리스트가 자동완성 힌트로 표시됨
                return listOf("start", "makeSpawn","addSubTeam").filter { it.startsWith(args[0]) }
            }
        }
        else if (command.name.equals("changenick", ignoreCase = true)) {
            if (args.size == 1) {
                return listOf("reset").filter { it.startsWith(args[0]) }
            }
        }
        return null
    }
}
