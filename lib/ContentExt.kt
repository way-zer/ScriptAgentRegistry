package ktor.lib

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.util.DSLBuilder
import io.ktor.application.*

val Script.webInit by DSLBuilder.callbackKey<Application.() -> Unit>()