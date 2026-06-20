plugins {
    application
}

group = "com.crpg.ebb"
version = "0.1.0-dev"

dependencies {
    implementation("com.openai:openai-java:4.39.1")
    implementation("com.h2database:h2:2.4.240")
    implementation("com.google.code.gson:gson:2.13.2")
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
