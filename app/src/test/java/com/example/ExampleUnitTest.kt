package com.example

import com.example.data.workflow.WorkflowTemplate
import com.example.util.SampleProjectGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testWorkflowTemplateContainsEssentialSteps() {
    val yaml = WorkflowTemplate.WORKFLOW_YAML
    assertTrue("Workflow deve conter setup de Java JDK 17", yaml.contains("actions/setup-java@v4"))
    assertTrue("Workflow deve conter setup de Gradle", yaml.contains("gradle/actions/setup-gradle@v4"))
    assertTrue("Workflow deve compilar assembleDebug", yaml.contains("assembleDebug"))
    assertTrue("Workflow deve empacotar e publicar artefato APK", yaml.contains("actions/upload-artifact@v4"))
    assertTrue("Workflow deve conter geração de keystores", yaml.contains("debug.keystore"))
  }

  @Test
  fun testSampleProjectGeneratorCreatesValidAndroidProject() {
    val result = SampleProjectGenerator.generateSampleProject()
    assertNotNull(result)
    assertTrue("Projeto de exemplo deve ser marcado como projeto Android válido", result.isValidAndroidProject)
    assertTrue("Projeto deve conter configurações do Gradle", result.hasSettingsGradle)
    assertTrue("Projeto deve conter módulo de app", result.hasAppModule)
    assertTrue("Projeto deve conter arquivos válidos", result.totalFiles >= 5)

    val filePaths = result.files.map { it.path }
    assertTrue(filePaths.contains("settings.gradle.kts"))
    assertTrue(filePaths.contains("build.gradle.kts"))
    assertTrue(filePaths.contains("app/build.gradle.kts"))
    assertTrue(filePaths.contains("app/src/main/AndroidManifest.xml"))
    assertTrue(filePaths.contains("app/src/main/java/com/sample/app/MainActivity.kt"))
  }
}
