plugins {
    // This module is primarily for build scripts and packaging
}

tasks.register("buildBootstrapArchive") {
    group = "build"
    description = "Packages the aarch64 runtime environment into bootstrap.tar.xz"
    
    doLast {
        println("Placeholder for building bootstrap.tar.xz")
    }
}
