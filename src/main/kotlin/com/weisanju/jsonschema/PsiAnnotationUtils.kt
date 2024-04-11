package com.weisanju.jsonschema

import com.intellij.psi.PsiModifierListOwner

class PsiAnnotationUtils {

    companion object {
        fun getAnnotationValue(annotation: String, value: String, element: PsiModifierListOwner): String? {
            return element.getAnnotation(annotation)?.findAttributeValue(value)?.text
        }
    }

}