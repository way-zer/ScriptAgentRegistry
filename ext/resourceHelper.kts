@file:Depends("wayzer/maps")

package wayzer.ext

import arc.struct.StringMap
import arc.util.Strings
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import cf.wayzer.placehold.PlaceHoldApi.with
import com.google.common.cache.CacheBuilder
import mindustry.game.Gamemode
import mindustry.io.SaveIO
import mindustry.maps.Map
import wayzer.MapInfo
import wayzer.MapManager
import wayzer.MapProvider
import wayzer.MapRegistry
import java.io.ByteArrayInputStream
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.util.logging.Level
import java.util.zip.InflaterInputStream
import mindustry.maps.Map as MdtMap

name = "资源站配套脚本"

val token by config.key("", "Mindustry资源站服务器Token")
val webRoot by config.key("https://api.mindustry.top", "Mindustry资源站Api")

val tokenOk get() = token.isNotBlank()

var MdtMap.resourceHash: String?
    get() = tags.get("resourceHash")
    set(v) {
        tags.put("resourceHash", v)
    }

fun JsonValue.toStringMap() = StringMap().apply {
    var node = child()
    do {
        put(node.name, node.toJson(JsonWriter.OutputType.minimal))
        node = node.next
    } while (node != null)
}

suspend fun httpGet(url: String, retry: Int = 3) = withContext(Dispatchers.IO) {
    var result: Result<ByteArray> = Result.failure(IllegalStateException("result not set"))
    repeat(retry + 1) {
        result = kotlin.runCatching {
            val stream = URL(url).openConnection()
                .apply { readTimeout = 1_000 }
                .getInputStream()
            runInterruptible { stream.readBytes() }
        }.onSuccess { return@withContext it }
        delay(1000)
    }
    result.getOrThrow()
}


MapRegistry.register(this, object : MapProvider() {
    val searchCache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build<String, List<MapInfo>>()!!

    fun newMapInfo(id: Int, hash: String, tags: StringMap, mode: String): MapInfo {
        val map = Map(customMapDirectory.child("unknown"), tags.getInt("width"), tags.getInt("height"), tags, true)
            .apply { resourceHash = hash }
        val mode2 = Gamemode.all.find { it.name.equals(mode, ignoreCase = true) }
            ?: if (mode.equals("unknown", true)) map.rules().mode() else Gamemode.survival
        return MapInfo(this, id, map, mode2)
    }

    override suspend fun searchMaps(search: String?): Collection<MapInfo> {
        if (!tokenOk) return emptyList()
        val mappedSearch = when (search) {
            "all", "display", "site", null -> ""
            "pvp", "attack", "survive" -> "@mode:${Strings.capitalize(search)}"
            else -> search
        }
        searchCache.getIfPresent(mappedSearch)?.let { return it }
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            val maps =
                httpGet("$webRoot/maps/list?prePage=100&search=${URLEncoder.encode(mappedSearch, "utf-8")}", retry = 1)
                    .let { JsonReader().parse(it.toString(Charsets.UTF_8)) }
                    .map {
                        val id = it.getInt("id")
                        val hash = it.getString("latest")
                        newMapInfo(id, hash, it.toStringMap(), it.getString("mode", "unknown"))
                    }
            searchCache.put(mappedSearch, maps)
            return maps
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Fail to searchMap($search)", e)
            return emptyList()
        }
    }

    override suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)?): MapInfo? {
        if (id !in 10000..99999) return null
        if (!tokenOk) {
            reply?.invoke("[red]本服未开启网络换图，请联系服主开启".with())
            return null
        }
        try {
            val info = httpGet("$webRoot/maps/thread/$id/latest")
                .let { JsonReader().parse(it.toString(Charsets.UTF_8)) }
            val hash = info.getString("hash")
            val tags = info.get("tags").toStringMap()
            return newMapInfo(id, hash, tags, info.getString("mode", "unknown"))
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Fail to findById($id)", e)
            return null
        }
    }

    override fun loadMap(map: MapInfo) {
        val hash = map.map.resourceHash ?: return MapManager.loadMap()
        val bs = runBlocking { httpGet("$webRoot/maps/$hash.msav", retry = 3) }
        @Suppress("INACCESSIBLE_TYPE")
        SaveIO.load(InflaterInputStream(ByteArrayInputStream(bs)), world.filterContext(map.map))
    }
})
