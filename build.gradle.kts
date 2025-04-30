plugins {
    kotlin("jvm")
}

dependencies {
    defineModule("bootStrap") {}
    defineModule("coreBukkit") {
        dependsModule("coreLibrary", parent!!)
        api("dev.folia:folia-api:1.21.4-R0.1-SNAPSHOT")
    }
    defineModule("superItem") {
        dependsModule("coreBukkit")
        implementation("de.tr7zw:item-nbt-api-plugin:2.15.0")
    }
}