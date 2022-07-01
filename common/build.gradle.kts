plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("com.squareup.sqldelight")
    id("com.codingfeline.buildkonfig")
}

kotlin {
    android()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "common"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.insert-koin:koin-core:3.2.0")
                implementation("com.squareup.sqldelight:runtime:1.5.3")
                implementation("com.squareup.sqldelight:coroutines-extensions:1.5.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:android-driver:1.5.3")

            }
        }
        val androidTest by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)

            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.3")
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    compileSdk = 31
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 28
    }
}

buildkonfig {
    packageName = "com.ludoscity.findmybikes"

    exposeObjectWithName = "FindmybikesBuildKonfig"

    /*********
     * Flavored TargetConfig > TargetConfig > Flavored DefaultConfig > DefaultConfig
     *********/

    // default config is required
    defaultConfigs {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "DATABASE_NAME", "findmybikes-database")
    }
    // flavor is passed as a first argument of defaultConfigs
    defaultConfigs("debug") {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "DATABASE_NAME", "findmybikes-database-dev")
    }

    defaultConfigs("release") {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "DATABASE_NAME", "findmybikes-database")
    }

    /*targetConfigs {
        create("android") {
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "name", "valueAndroid")
        }

        create("ios") {
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "name", "valueIos")
        }
    }*/
    // flavor s passed as a first argument of targetConfigs
    /*targetConfigs("dev") {
        create("ios") {
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "name", "findmybikes-database-dev")
        }

        create("android") {
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "name", "findmybikes-database-dev")
        }
    }

    targetConfigs("release") {
        create("ios") {
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "name", "findmybikes-database")
        }

        create("android") {
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "name", "findmybikes-database")
        }
    }*/
}

sqldelight {
    database("FindmybikesDatabase") {
        packageName = "com.ludoscity.findmybikes.common.data.database"
    }
}
