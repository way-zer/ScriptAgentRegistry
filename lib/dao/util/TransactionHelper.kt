package wayzer.lib.dao.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

object TransactionHelper {
    private val lateRun = mutableListOf<Transaction.() -> Unit>()
    fun lateUpdate(body: Transaction.() -> Unit) {
        lateRun.add(body)
    }

    fun flushAsync(scope: CoroutineScope) {
        if (lateRun.isEmpty()) return
        val list = lateRun.toList()
        lateRun.clear();
        scope.launch(Dispatchers.IO) {
            transaction {
                list.forEach { it() }
            }
        }
    }
}