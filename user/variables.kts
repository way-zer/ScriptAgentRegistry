package wayzer.user

import cf.wayzer.placehold.DynamicVar
import java.time.Duration
import java.util.*

registerVarForType<Player>().apply {
    registerChild("profile", "统一账号信息(可能不存在)", DynamicVar.obj { UserService.secureProfile(it) })
}

registerVarForType<PlayerProfile>().apply {
    registerChild("id", "绑定的账号ID(qq)", DynamicVar.obj { it.qq })
    registerChild("totalExp", "总经验", DynamicVar.obj { it.totalExp })
    registerChild("onlineTime", "总在线时间", DynamicVar.obj { Duration.ofSeconds(it.totalTime.toLong()) })
    registerChild("registerTime", "注册时间", DynamicVar.obj { Date.from(it.registerTime) })
    registerChild("lastTime", "账号最后登录时间", DynamicVar.obj { Date.from(it.lastTime) })
}