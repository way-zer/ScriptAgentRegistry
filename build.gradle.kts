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
        dependsOnModule("coreLibrary", parent!!)
        api("dev.folia:folia-api")
    }
    defineModule("superItem") {
        dependsOnModule("coreBukkit")
        implementation("de.tr7zw:item-nbt-api-plugin:2.15.0")
    }
}