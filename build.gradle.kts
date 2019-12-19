import org.zaproxy.gradle.addon.AddOnPlugin
import org.zaproxy.gradle.addon.AddOnStatus
import org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml
import org.zaproxy.gradle.addon.misc.CreateGitHubRelease
import org.zaproxy.gradle.addon.misc.ExtractLatestChangesFromChangelog

plugins {
    `java-library`
    id("org.zaproxy.add-on") version "0.2.0"
}

repositories {
    mavenCentral()
}

version = "1.0.1"
description = "Colors history table items based on tags"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

zapAddOn {
    addOnName.set("Neonmarker")
    addOnStatus.set(AddOnStatus.ALPHA)
    zapVersion.set("2.8.0")

    releaseLink.set("https://github.com/kingthorin/neonmarker/compare/v@PREVIOUS_VERSION@...v@CURRENT_VERSION@")
    unreleasedLink.set("https://github.com/kingthorin/neonmarker/compare/v@CURRENT_VERSION@...HEAD")

    manifest {
        author.set("Juha Kivekäs, kingthorin")
        url.set("https://github.com/kingthorin/neonmarker")
        changesFile.set(tasks.named<ConvertMarkdownToHtml>("generateManifestChanges").flatMap { it.html })

        helpSet {
            baseName.set("help%LC%.helpset")
            localeToken.set("%LC%")
        }
    }
}
