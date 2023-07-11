# PluginCore
[![](https://img.shields.io/badge/dynamic/json?color=blue&label=JitPack&query=latestOk&url=https://jitpack.io/api/builds/com.github.Fxcilities/PluginCore/latest&style=for-the-badge)](https://jitpack.io/#Fxcilities/PluginCore)
[![](https://img.shields.io/badge/dynamic/json?color=blue&label=JavaDoc&query=latestOk&url=https://jitpack.io/api/builds/com.github.Fxcilities/PluginCore/latest&style=for-the-badge)](https://javadoc.jitpack.io/com/github/Fxcilities/PluginCore/latest/javadoc/index.html)
[![](https://img.shields.io/github/workflow/status/Fxcilities/PluginCore/Java%20CI%20with%20Gradle?color=blue&style=for-the-badge)](https://github.com/Fxcilities/PluginCore/actions)
[![CodeFactor](https://www.codefactor.io/repository/github/fxcilities/plugincore/badge?style=for-the-badge)](https://www.codefactor.io/repository/github/fxcilities/plugincore)

A core for all my spigot plugins

## Getting started

### If you use gradle, add this.
```kotlin
```repositories {
      maven { url = "https://jitpack.io" }
}

dependencies {
      implementation("com.github.Fxcilities:PluginCore:VERSION")
}

# If you use maven, add this.
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Fxcilities</groupId>
        <artifactId>PluginCore</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>```

#m
kotlin
Copy code
# PluginCore
[![](https://img.shields.io/badge/dynamic/json?color=blue&label=JitPack&query=latestOk&url=https://jitpack.io/api/builds/com.github.Fxcilities/PluginCore/latest&style=for-the-badge)](https://jitpack.io/#Fxcilities/PluginCore)
[![](https://img.shields.io/badge/dynamic/json?color=blue&label=JavaDoc&query=latestOk&url=https://jitpack.io/api/builds/com.github.Fxcilities/PluginCore/latest&style=for-the-badge)](https://javadoc.jitpack.io/com/github/Fxcilities/PluginCore/latest/javadoc/index.html)
[![](https://img.shields.io/github/workflow/status/Fxcilities/PluginCore/Java%20CI%20with%20Gradle?color=blue&style=for-the-badge)](https://github.com/Fxcilities/PluginCore/actions)
[![CodeFactor](https://www.codefactor.io/repository/github/fxcilities/plugincore/badge?style=for-the-badge)](https://www.codefactor.io/repository/github/fxcilities/plugincore)

A core for all my spigot plugins

## Getting started

### If you use gradle, add this.
```kotlin
repositories {
      maven { url = "https://jitpack.io" }
}

dependencies {
      implementation("com.github.Fxcilities:PluginCore:VERSION")
}
```
# If you use maven, add this.

```kotlin
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Fxcilities</groupId>
        <artifactId>PluginCore</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```kotlin
```
# If you use a dependency manager not listed, check the jitpack for more examples.
NOTE: Make sure to shadow the PluginCore dependency into your plugin build.
Examples
Basic plugin with a command
MyPlugin.kt

```kotlin
class MyPlugin : Core() {

    override fun onPluginEnable() {
        console.print("Hello world!")

        // Initialize commands
        MyCommand()
    }

    override fun onPluginDisable() {
        console.print("Goodbye world!")
    }

    override fun getPrefix(): String {
        return "&bMyPlugin > &f"
    }

    override fun getPluginVersion(): String {
        return "v1.0"
    }

    override fun getPluginName(): String {
        return "MyPlugin"
    }

    override fun getPluginAuthors(): Array<String> {
        return arrayOf(
            "Mario",
            "Luigi"
        )
    }
}
```kotlin
```
# MyCommand.kt
```kotlin
class MyCommand : ServerCommand(
    "hello", "says hello world", "/hello", true, listOf("helloworld", "world")
) {
    init {
        // Optional
        setCooldownDuration(5, TimeUnit.SECONDS) // Five second cooldown
    }

    override fun onCommand() {
        say(true, "&aHello world!") // true to show the prefix of the plugin
    }
}
```kotlin
```
# Credit

## ExpiringMap
## PlayerScoreboard
## Some Ideas
