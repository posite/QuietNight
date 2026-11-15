package com.example.quietnight.data

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class BackupRulesTest {
    @Test
    fun legacyBackupExcludesPrivateRecordingsAndTheirDatabase() {
        assertPrivateDataExcluded(readRules("backup_rules.xml").documentElement)
    }

    @Test
    fun cloudBackupAndDeviceTransferExcludePrivateRecordingsAndTheirDatabase() {
        val document = readRules("data_extraction_rules.xml")
        listOf("cloud-backup", "device-transfer").forEach { name ->
            val nodes = document.getElementsByTagName(name)
            assertTrue("$name 규칙이 필요합니다", nodes.length == 1)
            assertPrivateDataExcluded(nodes.item(0) as Element)
        }
    }

    private fun readRules(name: String) = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder().parse(File("src/main/res/xml/$name"))

    private fun assertPrivateDataExcluded(parent: Element) {
        val nodes = parent.getElementsByTagName("exclude")
        val exclusions = (0 until nodes.length).map { nodes.item(it) as Element }
            .map { it.getAttribute("domain") to it.getAttribute("path") }
        assertTrue("녹음 파일은 백업에서 제외해야 합니다", "file" to "snore_clips/" in exclusions)
        assertTrue("녹음 경로와 수면 기록은 백업에서 제외해야 합니다", "database" to "." in exclusions)
    }
}
