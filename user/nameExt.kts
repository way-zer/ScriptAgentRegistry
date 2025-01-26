package wayzer.user

import cf.wayzer.placehold.DynamicVar
import coreLibrary.lib.registerVarForType
import coreLibrary.lib.util.loop
import coreLibrary.lib.with
import coreMindustry.lib.game
import coreMindustry.lib.listen
import mindustry.game.EventType
import mindustry.gen.Groups
import mindustry.gen.Player

@Savable(serializable = false)
val realName = mutableMapOf<String, String>()
customLoad(::realName) { realName.putAll(it) }

registerVarForType<Player>().apply {
    registerChild(
        "prefix", "名字前缀,可通过prefix.xxx变量注册", DynamicVar.obj { resolveVar(it, "prefix.*.toString", "") })
    registerChild(
        "suffix", "名字后缀,可通过suffix.xxx变量注册", DynamicVar.obj { resolveVar(it, "suffix.*.toString", "") })
}


fun Player.updateName() {
    name = "[white]{player.prefix}[]{name}[white]{player.suffix}".with(
        "player" to this,
        "name" to (realName[uuid()] ?: "NotInit")
    ).toString()
}

listen<EventType.PlayerConnect> {
    val p = it.player
    realName[p.uuid()] = p.name
    p.updateName()
}
onEnable {
    loop(Dispatchers.game) {
        delay(5000)
        Groups.player.forEach { it.updateName() }
    }
}