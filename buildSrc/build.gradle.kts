buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }

    dependencies {
        //classpath(Deps.SqlDelight.gradle)
        //classpath(Deps.cocoapodsExt)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven(url = "https://dl.bintray.com/ekito/koin")
        maven(url = "https://kotlin.bintray.com/kotlinx")
    }
}

/*tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}*/

