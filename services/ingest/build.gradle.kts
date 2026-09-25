plugins {
	kotlin("jvm")
	application
}

group = "com.energetica"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

application {
	mainClass.set("com.energetica.enron.ingest.IngestApplicationKt")
}

val schemaFile = project.file("../db/schema.sql")

tasks.processResources {
	from(schemaFile) {
		into("services/db")
	}
}

dependencies {
	implementation("org.apache.commons:commons-csv:1.14.0")
	implementation("org.apache.james:apache-mime4j-dom:0.8.12")
	implementation("org.postgresql:postgresql:42.7.7")
	implementation("org.slf4j:slf4j-simple:2.0.17")
	testImplementation(kotlin("test-junit5"))
	testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.testcontainers:postgresql:1.21.3")
	testImplementation("org.testcontainers:junit-jupiter:1.21.3")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
	jvmArgs("-Xmx256m")
}
