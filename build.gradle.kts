plugins {
    id("java")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.mockito:mockito-inline:4.0.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.3") // Mock server for OkHttp
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.13.2") // For ObjectMapper
}

tasks.test {
    useJUnitPlatform()
}
