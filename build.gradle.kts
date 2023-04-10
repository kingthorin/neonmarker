import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import net.ltgt.gradle.errorprone.errorprone
import org.zaproxy.gradle.addon.AddOnStatus
import org.zaproxy.gradle.addon.internal.model.GitHubUser
import org.zaproxy.gradle.addon.internal.model.ProjectInfo
import org.zaproxy.gradle.addon.internal.model.ReleaseState
import org.zaproxy.gradle.addon.internal.tasks.GenerateReleaseStateLastCommit
import org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml

plugins {
    id("com.diffplug.spotless") version "6.14.1"
    id("com.github.ben-manes.versions") version "0.45.0"
    id("net.ltgt.errorprone") version "3.0.1"
    `java-library`
    id("org.zaproxy.add-on") version "0.8.0"
    id("org.zaproxy.common") version "0.1.0"
}

repositories {
    mavenCentral()
}

java {
    val javaVersion = JavaVersion.VERSION_11
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

dependencies {
    "errorprone"("com.google.errorprone:error_prone_core:2.18.0")
}

spotless {
    kotlinGradle {
        ktlint()
    }
    java {
        // Don't enforce the license, just the format.
        clearSteps()
        googleJavaFormat("1.7").aosp()
    }
    format("html", {
        eclipseWtp(EclipseWtpFormatterStep.HTML)
        target(
            fileTree(projectDir) {
                include("src/**/*.html")
            },
        )
    })
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "utf-8"
    options.errorprone {
        error(
            "MethodCanBeStatic",
            "WildcardImport",
        )
    }
}

description = "Colors history table items based on tags"

zapAddOn {
    addOnName.set("Neonmarker")
    addOnStatus.set(AddOnStatus.ALPHA)
    zapVersion.set("2.10.0")

    releaseLink.set("https://github.com/kingthorin/neonmarker/compare/v@PREVIOUS_VERSION@...v@CURRENT_VERSION@")
    unreleasedLink.set("https://github.com/kingthorin/neonmarker/compare/v@CURRENT_VERSION@...HEAD")

    manifest {
        author.set("Juha Kivekäs, Kingthorin")
        url.set("https://www.zaproxy.org/docs/desktop/addons/neonmarker/")
        repo.set("https://github.com/kingthorin/neonmarker")
        changesFile.set(tasks.named<ConvertMarkdownToHtml>("generateManifestChanges").flatMap { it.html })

        helpSet {
            baseName.set("help%LC%.helpset")
            localeToken.set("%LC%")
        }
    }

    gitHubRelease {
        user.set(GitHubUser("kingthorin", "kingthorin@users.noreply.github.com", System.getenv("AUTH_TOKEN")))
    }
}

val projectInfo = ProjectInfo.from(project)
val generateReleaseStateLastCommit by tasks.registering(GenerateReleaseStateLastCommit::class) {
    projects.set(listOf(projectInfo))
}

val releaseAddOn by tasks.registering {
    if (ReleaseState.read(projectInfo).isNewRelease()) {
        dependsOn(tasks.createRelease)
        dependsOn(tasks.handleRelease)
        dependsOn(tasks.createPullRequestNextDevIter)
    }
}
