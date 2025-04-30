@file:Suppress("UnusedReceiverParameter", "unused")

package coreBukkit.lib

import cf.wayzer.scriptAgent.Config
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.*
import kotlin.coroutines.cancellation.CancellationException

sealed class BukkitDispatcher : CoroutineDispatcher() {
    @Volatile
    private var inBlocking = false
    private var blockingQueue = ConcurrentLinkedQueue<Runnable>()


    final override fun isDispatchNeeded(context: CoroutineContext): Boolean = !inTargetThread()
    final override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (inBlocking) {
            blockingQueue.add(block)
            return
        }
        doDispatch(context, block)
    }

    protected abstract fun inTargetThread(): Boolean
    protected abstract fun doDispatch(context: CoroutineContext, block: Runnable)

    fun <T> safeBlocking(block: suspend CoroutineScope.() -> T): T {
        check(inTargetThread()) { "safeBlocking must be called in target thread, current ${Thread.currentThread()}" }
        if (inBlocking) return runBlocking(Dispatchers.game, block)
        inBlocking = true
        return runBlocking {
            launch {
                while (inBlocking || blockingQueue.isNotEmpty()) {
                    blockingQueue.poll()?.run() ?: yield()
                }
            }
            try {
                withContext(Dispatchers.game, block)
            } finally {
                inBlocking = false
            }
        }
    }

    suspend fun nextTick() {
        coroutineContext.ensureActive()
        suspendCoroutine {
            doDispatch(it.context) { it.resume(Unit) }
        }
    }

    data object Main : BukkitDispatcher() {
        override fun inTargetThread(): Boolean = Bukkit.isPrimaryThread()

        override fun doDispatch(context: CoroutineContext, block: Runnable) {
            Bukkit.getGlobalRegionScheduler().execute(Config.pluginMain, block)
        }
    }

    data class Region(val location: Location) : BukkitDispatcher() {
        override fun inTargetThread(): Boolean = Bukkit.isOwnedByCurrentRegion(location)
        override fun doDispatch(context: CoroutineContext, block: Runnable) {
            Bukkit.getRegionScheduler().execute(Config.pluginMain, location, block)
        }
    }

    data class EntityHasRemovedException(val entity: org.bukkit.entity.Entity) :
        CancellationException("Entity $entity has removed")

    data class Entity(val entity: org.bukkit.entity.Entity) : BukkitDispatcher() {
        override fun inTargetThread(): Boolean = Bukkit.isOwnedByCurrentRegion(entity)
        override fun doDispatch(context: CoroutineContext, block: Runnable) {
            val success = entity.scheduler.execute(Config.pluginMain, block, {
                context.cancel(EntityHasRemovedException(entity))
                Dispatchers.Unconfined.dispatch(context, block)
            }, 0)
            if (!success) {
                context.cancel(EntityHasRemovedException(entity))
                Dispatchers.Unconfined.dispatch(context, block)
            }
        }
    }
}

val Dispatchers.game get() = BukkitDispatcher.Main
fun Dispatchers.region(block: Block) = BukkitDispatcher.Region(block.location)
fun Dispatchers.region(location: Location) = BukkitDispatcher.Region(location)
/** 获取实体当前所在region的[CoroutineDispatcher]。
 * 该[CoroutineDispatcher]不随实体传送变化，如果追踪实体请使用[Dispatchers.entity] */
fun Dispatchers.region(entity: Entity) = BukkitDispatcher.Region(entity.location)
fun Dispatchers.entity(entity: Entity) = BukkitDispatcher.Entity(entity)

suspend fun nextTick() {
    val dispatcher = coroutineContext[ContinuationInterceptor] as? BukkitDispatcher
        ?: error("nextTick only be invoke inside BukkitDispatcher context.")
    dispatcher.nextTick()
}