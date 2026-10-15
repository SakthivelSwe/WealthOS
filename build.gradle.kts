buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.postgresql:postgresql:42.7.3")
        classpath("org.flywaydb:flyway-database-postgresql:10.10.0")
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    id("org.flywaydb.flyway") version "10.10.0"
}

flyway {
    val rawFlywayUrl = (project.findProperty("flyway.url") as? String)
        ?: System.getenv("SUPABASE_DB_URL")
        ?: "jdbc:postgresql://db.zahzagnindkkttdpmqii.supabase.co:5432/postgres"
    url = when {
        rawFlywayUrl.startsWith("postgres://") -> rawFlywayUrl.replaceFirst("postgres://", "jdbc:postgresql://")
        rawFlywayUrl.startsWith("postgresql://") -> rawFlywayUrl.replaceFirst("postgresql://", "jdbc:postgresql://")
        else -> rawFlywayUrl
    }
    user = (project.findProperty("flyway.user") as? String)
        ?: System.getenv("SUPABASE_DB_USER")
        ?: "postgres"
    password = (project.findProperty("flyway.password") as? String)
        ?: System.getenv("SUPABASE_DB_PASSWORD")
        ?: ""
    locations = arrayOf("filesystem:supabase/migrations")
    baselineOnMigrate = true
    sqlMigrationPrefix = ""
    sqlMigrationSeparator = "_"
}
