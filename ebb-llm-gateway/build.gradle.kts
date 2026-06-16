plugins {
    application
}

group = "com.crpg.ebb"
version = "0.1.0-dev"

dependencies {
    implementation("com.openai:openai-java:4.39.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

application {
    mainClass.set("com.crpg.ebb.gateway.GatewayMain")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<JavaExec>("gatewaySmoke") {
    group = "verification"
    description = "Run the dependency-free P36 gateway endpoint smoke test."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.crpg.ebb.gateway.GatewaySmoke")
}

tasks.named("check") {
    dependsOn("gatewaySmoke")
}
