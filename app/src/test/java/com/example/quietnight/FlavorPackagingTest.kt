package com.example.quietnight

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class FlavorPackagingTest {
    private val android = "http://schemas.android.com/apk/res/android"

    @Test
    fun commonManifestSharesApplicationWithoutDeviceSpecificLauncher() {
        assertEquals("com.example.quietnight.QuietSleepApplication",
            elements("main", "application").single().getAttributeNS(android, "name"))
        assertTrue(elements("main", "activity").isEmpty())
        assertTrue(elements("main", "uses-feature").isEmpty())
    }

    @Test
    fun phoneFlavorHasOnlyThePhoneLauncher() {
        assertEquals(listOf("com.example.quietnight.ui.MainActivity"),
            elements("phone", "activity").map { it.getAttributeNS(android, "name") })
        assertTrue(elements("phone", "uses-feature").none {
            it.getAttributeNS(android, "name") == "android.hardware.type.watch"
        })
        assertTrue(elements("phone", "meta-data").none {
            it.getAttributeNS(android, "name") == "com.google.android.wearable.standalone"
        })
    }

    @Test
    fun wearFlavorHasOnlyTheStandaloneWatchLauncher() {
        assertTrue(elements("wear", "uses-feature").any {
            it.getAttributeNS(android, "name") == "android.hardware.type.watch" &&
                it.getAttributeNS(android, "required") != "false"
        })
        assertTrue(elements("wear", "meta-data").any {
            it.getAttributeNS(android, "name") == "com.google.android.wearable.standalone" &&
                it.getAttributeNS(android, "value") == "true"
        })
        val activities = elements("wear", "activity")
        assertEquals(listOf("com.example.quietnight.wear.WearActivity"),
            activities.map { it.getAttributeNS(android, "name") })
        assertEquals("true", activities.single().getAttributeNS(android, "exported"))
        assertTrue(elements("wear", "uses-feature").any {
            it.getAttributeNS(android, "name") == "android.hardware.microphone" &&
                it.getAttributeNS(android, "required") == "true"
        })
    }

    @Test
    fun watchFlavorDisablesClipsWhileSharedDefaultAllowsOptIn() {
        assertEquals("false", clipCapability(File("src/wear/res/values/config.xml")))
        assertEquals("true", clipCapability(File("../data/src/main/res/values/config.xml")))
    }

    @Test
    fun phoneAppliesSelectedThemeWhileKeepingNightPalette() {
        val themeSource = File(
            "../ui/src/phone/java/com/example/quietnight/ui/theme/Theme.kt"
        ).readText()
        val activitySource = File(
            "../ui/src/phone/java/com/example/quietnight/ui/MainActivity.kt"
        ).readText()

        assertTrue(
            "시스템 선택은 기기의 라이트·다크 설정을 따라야 합니다.",
            themeSource.contains("isSystemInDarkTheme()")
        )
        assertTrue("저장한 테마를 앱 전체에 적용해야 합니다.",
            activitySource.contains("settings.themeMode"))
        listOf("HomeScreen.kt", "SettingsScreen.kt", "AnalyzeScreen.kt").forEach { name ->
            assertFalse("화면이 앱에서 선택한 테마를 덮어쓰면 안 됩니다: $name",
                File("../ui/src/phone/java/com/example/quietnight/ui/$name").readText()
                    .contains("QuietNightTheme {"))
        }
        assertTrue(
            "밤 테마 배경은 눈부시지 않은 짙은 남색이어야 합니다.",
            themeSource.contains("background = Color(0xFF101827)")
        )
        assertTrue(
            "카드는 배경보다 한 단계 밝아야 합니다.",
            themeSource.contains("surface = Color(0xFF192538)")
        )
        assertTrue(
            "어두운 배경 위의 시스템 바 아이콘은 밝게 표시되어야 합니다.",
            activitySource.contains("SystemBarStyle.dark")
        )
        assertEquals(
            "#FF101827",
            resourceValue(File("src/phone/res/values/colors.xml"), "color", "window_background")
        )
    }

    @Test
    fun phoneNightPaletteUsesReadableTextRoles() {
        val phoneUi = listOf(
            "HomeScreen.kt",
            "MonitorScreen.kt",
            "SettingsScreen.kt",
            "AnalyzeScreen.kt",
            "DayDetailDialog.kt",
            "MainActivity.kt"
        ).joinToString("\n") { fileName ->
            File("../ui/src/phone/java/com/example/quietnight/ui/$fileName").readText()
        }

        assertFalse(
            "밝은 강조색 버튼 위에 흰색 글자를 고정하면 대비가 부족합니다.",
            phoneUi.contains("color = Color.White")
        )
        assertFalse(
            "outline 색상은 경계선용이며 작은 보조 문구에는 사용하지 않습니다.",
            phoneUi.contains("color = MaterialTheme.colorScheme.outline")
        )
        assertFalse(
            "하단 탐색 글자와 아이콘도 테마의 가독성 색상을 사용해야 합니다.",
            phoneUi.contains("else Color.Gray")
        )
    }

    @Test
    fun nightJournalProvidesReadableRootContentColor() {
        val journalSource = File(
            "../ui/src/phone/java/com/example/quietnight/ui/NightJournal.kt"
        ).readText()

        assertTrue(
            "분석 화면은 카드 밖 텍스트에도 밝은 본문색을 제공해야 합니다.",
            journalSource.contains("contentColor = MaterialTheme.colorScheme.onBackground")
        )
        assertTrue(
            "분석 화면 루트는 밤 테마 배경을 직접 그려야 합니다.",
            journalSource.contains("color = MaterialTheme.colorScheme.background")
        )
    }

    private fun elements(sourceSet: String, tag: String): List<Element> {
        val document = parse(File("src/$sourceSet/AndroidManifest.xml"))
        return document.getElementsByTagName(tag).let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
    }

    private fun clipCapability(file: File): String {
        val nodes = parse(file).getElementsByTagName("bool")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .single { it.getAttribute("name") == "supports_snore_clips" }.textContent.trim()
    }

    private fun resourceValue(file: File, tag: String, name: String): String {
        val nodes = parse(file).getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .single { it.getAttribute("name") == name }.textContent.trim()
    }

    private fun parse(file: File): org.w3c.dom.Document {
        assertTrue("소스 세트 파일이 필요합니다: $file", file.isFile)
        return DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file)
    }
}
