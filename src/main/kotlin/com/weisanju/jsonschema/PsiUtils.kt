package com.weisanju.jsonschema

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

class PsiUtils {
    companion object {
        fun getSelected(event: AnActionEvent) {
            //获取当前文件所处的JAVA类
            val editor = event.dataContext.getData(CommonDataKeys.EDITOR)!!

            val editorFile = event.dataContext.getData(CommonDataKeys.PSI_FILE)

            val referenceAt: PsiElement? = editorFile?.findElementAt(editor.caretModel.offset)

            val psiClass: PsiClass = PsiTreeUtil.getContextOfType(referenceAt, PsiClass::class.java)!!

            val selectMethod = PsiTreeUtil.getContextOfType(referenceAt, PsiMethod::class.java)!!

            val requestPath1 = PsiAnnotationUtils.getAnnotationValue(
                "org.springframework.web.bind.annotation.RequestMapping",
                "value",
                psiClass
            )

            val requestPath2 = PsiAnnotationUtils.getAnnotationValue(
                "org.springframework.web.bind.annotation.RequestMapping",
                "value",
                selectMethod
            )

            Messages.showMessageDialog(
                event.project,
                "class: ${psiClass.name}, method: ${selectMethod?.name}, requestPath: $requestPath1, $requestPath2",
                "Greeting",
                Messages.getInformationIcon()
            )
        }
    }
}