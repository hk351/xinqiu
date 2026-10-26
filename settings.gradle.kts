pluginManagement {
  repositories {
    // 阿里云镜像
    maven { url = uri("https://maven.aliyun.com/repository/google")}
    maven { url=uri ("https://maven.aliyun.com/repository/releases")}
    maven { url=uri ("https://maven.aliyun.com/repository/central")}
    maven { url=uri ("https://maven.aliyun.com/repository/gradle-plugin")}
    maven { url=uri ("https://www.jitpack.io")}
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    // 阿里云镜像
    maven { url = uri("https://maven.aliyun.com/repository/google")}
    maven { url=uri ("https://maven.aliyun.com/repository/releases")}
    maven { url=uri ("https://maven.aliyun.com/repository/central")}
    maven { url=uri ("https://maven.aliyun.com/repository/gradle-plugin")}
    maven { url=uri ("https://www.jitpack.io")}
  }
}

rootProject.name = "ydhl2026"
include(":app")
 