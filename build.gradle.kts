plugins {
    kotlin("jvm") version "2.3.0"
    id("me.champeau.jmh") version "0.7.3"
}

group = "com.bokyo"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.ta4j:ta4j-core:0.22.3")

    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-framework-engine:5.9.1")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"))
}

tasks.withType<Test> {
    jvmArgs("--add-modules", "jdk.incubator.vector")
}

tasks.withType<JavaExec> {
    jvmArgs("--add-modules", "jdk.incubator.vector")
}

jmh {
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(2)
    resultFormat.set("JSON")
    jvmArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"))
}