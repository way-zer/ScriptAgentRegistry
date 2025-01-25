package wayzer.user

import coreLibrary.DBApi
import coreLibrary.DBApi.DB.registerTable
import org.jetbrains.exposed.sql.transactions.transaction

name = "通知服务"

/** Should call in [Dispatchers.IO] */
fun notify(profile: PlayerProfile, message: String, params: Map<String, String>, broadcast: Boolean = false) {
    transaction {
        NotificationEntity.new(profile, message, params, broadcast)
    }
}
export(::notify)


fun List<NotificationEntity>.notify(profile: PlayerProfile) = launch(Dispatchers.game) {
    val players = Groups.player.asIterable().filter { UserService.secureProfile(it) == profile }
    forEach {
        if (it.broadcast)
            broadcast(
                it.message.with(
                    *it.params.map { e -> e.key to e.value }.toTypedArray(),
                    "player" to players.first()
                )
            )
        else players.forEach { p ->
            p.sendMessage(it.message.with(*it.params.map { e -> e.key to e.value }.toTypedArray(), "player" to p))
        }
    }
}

registerTable(NotificationEntity.T, NotificationEntity.TimeTable)
onEnable {
    loop {
        DBApi.DB.awaitInit()
        delay(5000)
        val online = Groups.player.mapNotNull { UserService.secureProfile(it) }.toSet()
        online.forEach { profile ->
            transaction {
                NotificationEntity.getNew(profile).toList()
            }.takeUnless { it.isEmpty() }?.notify(profile)
        }
    }
}