package com.example.quietnight

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureBoundaryTest {
    @Test
    fun moduleDependencyGraphPointsInwardOnly() {
        val expected = mapOf(
            "app" to setOf("domain", "data", "ui"),
            "data" to setOf("domain", "core"),
            "ui" to setOf("domain"),
            "domain" to emptySet(),
            "core" to emptySet()
        )
        val projectDependency = Regex("project\\(\":([a-z]+)\"\\)")
        expected.forEach { (module, dependencies) ->
            val buildFile = File("../$module/build.gradle.kts")
            assertTrue("$module 모듈이 필요합니다", buildFile.isFile)
            val build = buildFile.readText()
            val actual = projectDependency.findAll(build).map { it.groupValues[1] }.toSet()
            org.junit.Assert.assertEquals("$module 의존성 방향", dependencies, actual)
        }
    }

    @Test
    fun domainAndCoreAreIndependentJvmModules() {
        listOf("domain", "core").forEach { module ->
            val buildFile = File("../$module/build.gradle.kts")
            assertTrue("$module 모듈이 필요합니다", buildFile.isFile)
            val build = buildFile.readText()
            assertTrue("$module 는 순수 Kotlin/JVM 모듈이어야 합니다",
                build.contains("libs.plugins.kotlin.jvm"))
            assertTrue("$module 에 Android 플러그인을 적용하면 안 됩니다",
                !build.contains("libs.plugins.android"))
        }
    }

    @Test
    fun domainHasNoAndroidOrPresentationDependencies() {
        val forbidden = Regex("^import (android\\.|androidx\\.|dagger\\.|javax\\.inject\\.|com\\.example\\.quietnight\\.(app|data|ui|service|core)\\.)", RegexOption.MULTILINE)
        val sources = File("../domain/src/main").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("도메인 코드를 비워서 경계 검증을 우회할 수 없습니다", sources.isNotEmpty())
        val violations = sources.filter { forbidden.containsMatchIn(it.readText()) }
        assertTrue("도메인에서 플랫폼·저장소·UI 구현을 참조하면 안 됩니다: $violations", violations.isEmpty())
    }

    @Test
    fun coreContainsTechnicalCodeWithoutSleepDomainOrPlatformDependencies() {
        val sources = File("../core/src/main").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("공통 기술 코드는 core에 있어야 합니다", sources.isNotEmpty())
        val forbidden = Regex(
            "^(package|import) com\\.example\\.quietnight\\.(domain|app|data|ui|service)\\.|^import (android\\.|androidx\\.|dagger\\.|javax\\.inject\\.)",
            RegexOption.MULTILINE
        )
        val violations = sources.filter { forbidden.containsMatchIn(it.readText()) }
        assertTrue("core는 도메인·플랫폼에서 독립적이어야 합니다: $violations", violations.isEmpty())
    }

    @Test
    fun presentationUsesDomainInsteadOfStorageOrServiceImplementation() {
        val sources = File("../ui/src").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("화면과 ViewModel은 ui 모듈에 있어야 합니다", sources.isNotEmpty())
        val forbidden = Regex("com\\.example\\.quietnight\\.(data\\.|service\\.SnoreService)")
        assertTrue("UI는 도메인 인터페이스만 참조해야 합니다",
            sources.none { forbidden.containsMatchIn(it.readText()) })
    }
}
