@file:Depends("coreLibrary")
@file:Import("coreStandalone.lib.*", defaultImport = true)

package coreStandalone

name = "core module for standalone"
Commands.rootProvider.provide(this, RootCommands)