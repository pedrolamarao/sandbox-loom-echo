plugins {
    `java-library`
}

group = "br.dev.pedrolamarao.loom"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.0-alpha5")
    runtimeOnly("ch.qos.logback:logback-core:1.3.0-alpha10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
    useJUnitPlatform()
}