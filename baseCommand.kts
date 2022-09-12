package coreStandalone

command("stop", "结束程序") {
    body {
        launch(Job()) {
            ScriptManager.transaction {
                addAll()
                disable()
            }
        }
    }
}

command("gc", "垃圾回收") {
    body {
        fun getMemory() = Runtime.getRuntime().run { totalMemory() - freeMemory() } / 1024 / 1024
        val before = getMemory()
        System.gc()
        val after = getMemory()
        reply("GC完成,释放了{released}MB内存,当前{now}MB".with("released" to (before - after), "now" to after))
    }
}