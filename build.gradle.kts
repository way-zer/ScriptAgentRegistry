plugins {
    kotlin("jvm")
}

repositories {
    //folia
    maven(url = "https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    defineModule("bootStrap") {}
    defineModule("coreBukkit") {
        dependsModule("coreLibrary", parent!!)
        api("dev.folia:folia-api")
    }
    defineModule("superItem") {
        dependsModule("coreBukkit")
        implementation("de.tr7zw:item-nbt-api-plugin:2.15.0")
    }
}