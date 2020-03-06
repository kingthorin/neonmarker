import org.zaproxy.gradle.addon.AddOnPlugin
import org.zaproxy.gradle.addon.AddOnStatus
import org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml
import org.zaproxy.gradle.addon.misc.CreateGitHubRelease
import org.zaproxy.gradle.addon.misc.ExtractLatestChangesFromChangelog

plugins {
    id("com.diffplug.gradle.spotless") version "3.27.2"
    id("com.github.ben-manes.versions") version "0.28.0"
    `java-library`
    id("org.zaproxy.add-on") version "0.3.0"
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
        googleJavaFormat().aosp()
    }
}

tasks.withType<JavaCompile>().configureEach { options.encoding = "utf-8" }

version = "1.2.0"
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
}
