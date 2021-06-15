import org.zaproxy.gradle.addon.AddOnPlugin
import org.zaproxy.gradle.addon.AddOnStatus
import org.zaproxy.gradle.addon.internal.model.GitHubUser
import org.zaproxy.gradle.addon.internal.model.ProjectInfo
import org.zaproxy.gradle.addon.internal.model.ReleaseState
import org.zaproxy.gradle.addon.internal.tasks.GenerateReleaseStateLastCommit
import org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml

plugins {
    id("com.diffplug.spotless") version "5.12.1"
    id("com.github.ben-manes.versions") version "0.38.0"
    `java-library`
    id("org.zaproxy.add-on") version "0.6.0"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

spotless {
    java {
        // Don't enforce the license, just the format.
        clearSteps()
        googleJavaFormat("1.7").aosp()
    }
}

tasks.withType<JavaCompile>().configureEach { options.encoding = "utf-8" }

description = "Colors history table items based on tags"

zapAddOn {
    addOnName.set("Neonmarker")
    addOnStatus.set(AddOnStatus.ALPHA)
    zapVersion.set("2.8.0")

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
