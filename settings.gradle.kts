pluginManagement {
    repositories {
        // 阿里云 Google 镜像（最关键，解决 com.android.application）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云 Maven Central 镜像
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 阿里云 Gradle 插件镜像
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // 兜底（可留）
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}


rootProject.name = "MyFirstApplication"
include(":app")
 