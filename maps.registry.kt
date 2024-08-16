package wayzer

import arc.util.Log
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.emitAsync
import coreLibrary.lib.PlaceHoldString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mindustry.Vars
import mindustry.game.Gamemode
import mindustry.io.SaveIO
import mindustry.maps.Map

data class MapInfo(
    val provider: MapProvider, val id: Int, val map: Map, val mode: Gamemode
) {
    override fun equals(other: Any?): Boolean = other is MapInfo && (provider == other.provider && id == other.id)
    override fun hashCode(): Int = 31 * provider.hashCode() + id
}

abstract class MapProvider {
    abstract suspend fun searchMaps(search: String? = null): Collection<MapInfo>
    /**@param id may not exist in getMaps*/
    open suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)? = null): MapInfo? =
        searchMaps().find { it.id == id }

    open fun loadMap(map: MapInfo) {
        Vars.world.loadMap(map.map)
    }
}

class GetNextMapEvent(val previous: MapInfo?, var mapInfo: MapInfo) : Event, Event.Cancellable {
    override var cancelled: Boolean = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

object MapRegistry : MapProvider() {
    private val providers = mutableSetOf<MapProvider>()
    fun register(script: Script, provider: MapProvider) {
        script.onDisable {
            providers.remove(provider)
        }
        providers.add(provider)
    }

    override suspend fun searchMaps(search: String?): List<MapInfo> {
        @Suppress("NAME_SHADOWING")
        val search = search.takeUnless { it == "all" || it == "display" }
        return coroutineScope {
            providers.map { async { it.searchMaps(search) } }
                .flatMap { it.await() }
        }
    }

    /**Dispatch should be Dispatchers.game*/
    override suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)?): MapInfo? {
        return providers.firstNotNullOfOrNull { it.findById(id, reply) }
    }

    suspend fun nextMapInfo(
        previous: MapInfo? = null,
        mode: Gamemode = Gamemode.survival
    ): MapInfo {
        val maps = searchMaps().let { maps ->
            if (maps.isNotEmpty()) return@let maps
            Log.warn("服务器未安装地图,自动使用内置地图")
            searchMaps("@internal")
        }
        val next = maps.filter { it.mode == mode && it != previous }.randomOrNull() ?: maps.random()
        return GetNextMapEvent(previous, next).emitAsync().mapInfo
    }

    //not need register
    object SaveProvider : MapProvider() {
        override suspend fun searchMaps(search: String?): Collection<MapInfo> = emptyList()
        override fun loadMap(map: MapInfo) {
            SaveIO.load(map.map.file)
        }
    }
}