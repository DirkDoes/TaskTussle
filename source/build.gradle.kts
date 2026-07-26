import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()
base { archivesName = providers.gradleProperty("archives_base_name") }

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
	testImplementation(kotlin("test"))
}

tasks.processResources {
	inputs.property("version", version)
	filesMatching("fabric.mod.json") { expand("version" to version) }
}

tasks.withType<JavaCompile>().configureEach { options.release = 25 }
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_25 } }
java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}
tasks.test { useJUnitPlatform() }

loom {
	runs.named("server") {
		runDir("build/run")
		programArg("nogui")
	}
}

publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			artifactId = base.archivesName.get()
			from(components["java"])
		}
	}
}
