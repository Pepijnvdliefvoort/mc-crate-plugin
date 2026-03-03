plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "io.github.yourname"
version = "1.0.0"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.register<Copy>("copyToServer") {
    group = "paper"
    description = "Copies the plugin to the test server plugins folder"
    
    from(tasks.jar)
    into(file("run/plugins"))
    
    doLast {
        println("Plugin copied to run/plugins")
    }
}

tasks.runServer {
    minecraftVersion("1.21.11")
    
    jvmArgs("-Xmx2G", "-Xms2G")
    
    runDirectory(file("run"))
    
    // Download and run plugins you might need for testing
    // downloadPlugins {
    //     url("https://ci.lucko.me/job/LuckPerms/lastSuccessfulBuild/artifact/bukkit/loader/build/libs/LuckPerms-Bukkit-5.4.102.jar")
    // }
}
