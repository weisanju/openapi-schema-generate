package com.weisanju.jsonschema

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages


class HelloWorldAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val message = "Hello,World!"
        // Show dialog with message
        Messages.showMessageDialog(project, message, "Greeting", Messages.getInformationIcon())
    }
}

class OpenApiSchemaGenerate : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        PsiUtils.getSelected(e)
    }
}