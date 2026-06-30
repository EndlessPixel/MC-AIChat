plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "com.mcaichat"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.yaml:snakeyaml:2.2")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.shadowJar {
    archiveClassifier.set("")
    minimize {
        exclude(dependency("org.xerial:sqlite-jdbc"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
