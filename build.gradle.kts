plugins {
    base
}

allprojects {
    group = "com.dcim"
    version = "0.0.1-SNAPSHOT"
}

tasks.register("bootRun") {
    dependsOn(":server:bootRun")
    group = "application"
    description = "Run the Spring Boot server"
}
