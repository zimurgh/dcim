plugins {
    base
}

tasks.register<Exec>("pnpmInstall") {
    workingDir = projectDir
    commandLine("pnpm", "install")
}

tasks.register<Exec>("buildAngular") {
    dependsOn("pnpmInstall")
    workingDir = projectDir
    commandLine("pnpm", "run", "build")
    inputs.dir("src")
    inputs.files(
        "package.json",
        "pnpm-lock.yaml",
        "angular.json",
        "tsconfig.json",
        "tsconfig.app.json",
        ".postcssrc.json",
    )
    outputs.dir("dist")
}

tasks.named("build") {
    dependsOn("buildAngular")
}

tasks.register<Exec>("test") {
    dependsOn("pnpmInstall")
    workingDir = projectDir
    commandLine("pnpm", "test", "--", "--watch=false")
}
