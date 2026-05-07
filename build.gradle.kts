plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("Today")
}
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}
