package com.example.quietnight.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class UiPermissionTest {
    @Test fun phoneVibrationPreviewDeclaresItsOwnPermission() {
        val manifest = File("src/phone/AndroidManifest.xml")
        assertTrue("진동 미리보기를 제공하는 UI 모듈 자체에 권한 선언이 필요합니다", manifest.isFile)
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest)
        val permissions = document.getElementsByTagName("uses-permission")
        assertTrue((0 until permissions.length).any {
            (permissions.item(it) as Element).getAttributeNS(
                "http://schemas.android.com/apk/res/android", "name") == "android.permission.VIBRATE"
        })
    }
}
