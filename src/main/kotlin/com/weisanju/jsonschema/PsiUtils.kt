package com.weisanju.jsonschema

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.javadoc.PsiDocTokenImpl
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import java.util.*


class PsiUtils {
    companion object {
        fun getSelected(event: AnActionEvent): Pair<PsiClass, PsiMethod?> {
            //获取当前文件所处的JAVA类
            val editor = event.dataContext.getData(CommonDataKeys.EDITOR)!!

            val editorFile = event.dataContext.getData(CommonDataKeys.PSI_FILE)

            val referenceAt: PsiElement? = editorFile?.findElementAt(editor.caretModel.offset)

            val psiClass: PsiClass = PsiTreeUtil.getContextOfType(referenceAt, PsiClass::class.java)!!

            val selectMethod = PsiTreeUtil.getContextOfType(referenceAt, PsiMethod::class.java)

            return Pair(psiClass, selectMethod)
        }
    }
}


/**
 * PsiDocComment相关工具类
 */
object PsiDocCommentUtils {
    /**
     * 获取获取标记自定字段名
     *
     * @param element sdfdsf
     */
    fun getTagTextMap(element: PsiJavaDocumentedElement, tagName: String): Map<String, String> {
        val map: MutableMap<String, String> = HashMap()
        val tags = findTagsByName(element, tagName)
        for (tag in tags) {
            val elements = tag!!.dataElements
            //跳过空Tag
            if ((elements.size > 1 && elements[elements.size - 1] is PsiDocTokenImpl) && (elements[1] as PsiDocTokenImpl).tokenType.toString() == "DOC_COMMENT_DATA" && elements[1].text.trim { it <= ' ' }
                    .isEmpty()) {
                map[tagName] = elements[0].text.trim { it <= ' ' }
            } else if (elements.size == 1) {
                map[tagName] = elements[0].text.trim { it <= ' ' }
            } else if (elements.size >= 2) {
                val name = elements[0].text.trim { it <= ' ' }
                val description = elements[1].text.trim { it <= ' ' }
                map[name] = description
            }
        }
        return map
    }

    /**
     * 获取标记文本值
     */
    fun getTagText(element: PsiJavaDocumentedElement, tagName: String?): String? {
        val tag = findTagByName(element, tagName)
        if (tag != null) {
            val splits = tag.text.split("\\s".toRegex(), limit = 2).toTypedArray()
            if (splits.size > 1) {
                return splits[1]
            }
        }
        return null
    }

    /**
     * 获取文档标记内容
     */
    fun getDocCommentTagText(element: PsiJavaDocumentedElement, tagName: String?): String? {
        var text: String? = null
        val comment = element.docComment
        if (comment != null) {
            val tag = comment.findTagByName(tagName)
            if (tag != null && tag.valueElement != null) {
                val sb = StringBuilder()
                for (e in tag.dataElements) {
                    sb.append(e.text.trim { it <= ' ' })
                }
                text = sb.toString()
            }
        }
        return text
    }

    /**
     * 获取文档标题行
     */
    fun getDocCommentTitle(element: Any?): String? {
        if (element is PsiJavaDocumentedElement) {
            val comment = element.docComment
            if (comment != null) {
                return Arrays.stream(comment.descriptionElements)
                    .filter { o: PsiElement? -> o is PsiDocToken }
                    .map { obj: PsiElement -> obj.text }
                    .findFirst()
                    .map { obj: String -> obj.trim { it <= ' ' } }
                    .orElse(null)
            }
        }
        return null
    }

    /**
     * 获取文档注释上的标记
     */
    fun findTagByName(element: PsiJavaDocumentedElement, tagName: String?): PsiDocTag? {
        val comment = element.docComment
        if (comment != null) {
            return comment.findTagByName(tagName)
        }
        return null
    }


    /**
     * 获取文档注释上的标记
     */
    fun findTagsByName(element: PsiJavaDocumentedElement, tagName: String?): Array<PsiDocTag?> {
        val comment = element.docComment
        if (comment != null) {
            return comment.findTagsByName(tagName)
        }
        return arrayOfNulls(0)
    }

    /**
     * 获取注释中link标记的内容
     */
    fun getInlineLinkContent(element: PsiJavaDocumentedElement): String? {
        val comment = element.docComment ?: return null
        val linkElement = Arrays.stream(comment.descriptionElements)
            .filter { ele: PsiElement? ->
                (ele is PsiInlineDocTag) && ele.getText()
                    .startsWith("{@link")
            }
            .findFirst().orElse(null)
        if (linkElement == null) {
            return null
        }
        val text = linkElement.text
        return text.substring("{@link".length, text.length - 1).trim { it <= ' ' }
    }
}


object PsiAnnotationUtils {

    fun getAnnotationValue(annotation: String, value: String, element: PsiModifierListOwner): String? {
        return element.getAnnotation(annotation)?.findAttributeValue(value)?.text
    }

    fun annotationExists(annotation: String, element: PsiModifierListOwner): Boolean {
        return element.getAnnotation(annotation) != null
    }


    fun getAnnotation(annotation: String, element: PsiModifierListOwner): Map<String, String> {
        val psiAnnotation = element.getAnnotation(annotation) ?: return emptyMap()

        // 获取注解的所有值（PsiNameValuePair 对象列表）
        return psiAnnotation.parameterList.attributes.map {
            var attributeName = it.name
            val attributeValue = it.value

            if (attributeName == null) {
                // 如果没有指定名称（如 @MyAnnotation(value = "foo")），则默认为 "value"
                attributeName = "value"
            }

            val attributeValueText = if (attributeValue != null) attributeValue.text else ""

            attributeName to attributeValueText
        }.toMap()

    }
}


object PsiTypeUtils {


    fun getPsiTypeFromPsiClass(psiClass: PsiClass): PsiType {
        // 获取当前项目的 PsiElementFactory 实例
        val project = psiClass.project
        val factory = JavaPsiFacade.getInstance(project).elementFactory

        // 使用 PsiElementFactory 将 PsiClass 转换为 PsiType
        return factory.createType(psiClass)
    }

    fun isSimpleType(type: PsiType): Boolean {
        if (type is PsiPrimitiveType) {
            return true;
        }
        // 基础类型的包装类和 String 的全限定名
        // 如果是基础类型的包装类或 String，则返回 true
        return type.canonicalText in simpleTypes()
    }

    fun isJdkBuildIn(type: PsiType): Boolean {
        return type.canonicalText.startsWith("java.") || type.canonicalText.startsWith("javax.")
    }


    fun isInteger(type: PsiType): Boolean {
        val intType = arrayOf(
            PsiTypes.intType(), PsiTypes.longType(), PsiTypes.shortType(), PsiTypes.byteType(),
            PsiTypes.charType()
        )
        val intTypeStr =
            arrayOf("java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte", "java.lang.Character")
        return type in intType
                ||
                type.canonicalText in intTypeStr
    }


    fun isSimpleArray(type: PsiType?): Boolean {
        return type is PsiArrayType
    }


    fun isArray(type: PsiType?, project: Project): Boolean {
        return isSimpleArray(type) || isCollection(type, project);
    }


    fun getQualifiedName(type: PsiModifierListOwner?): String? {
        if (type is PsiClassType) {
            return type.resolve()?.qualifiedName!!
        }
        return getName(type)
    }

    fun isCollection(type: PsiType?, project: Project): Boolean {
        val list = PsiType.getTypeByName("java.util.List", project, GlobalSearchScope.allScope(project))
        if (type != null) {
            return list.isAssignableFrom(type)
        }
        return false
    }

    fun isAssignedFrom(project: Project, type: PsiModifierListOwner?, qualifiedName: String): Boolean {
        val list = PsiType.getTypeByName(qualifiedName, project, GlobalSearchScope.allScope(project))

        if (type != null && type is PsiParameter) {
            return list.isAssignableFrom(type.type)
        }

        return false
    }

    fun getName(
        element: PsiModifierListOwner?
    ): String? {
        return if (element is PsiNamedElement) {
            element.name
        } else {
            null
        }
    }

    fun isBoolean(type: PsiType): Boolean {
        return type.canonicalText == "java.lang.Boolean"
    }


    fun isNumber(type: PsiType): Boolean {
        //double or float
        return type.canonicalText == "java.lang.Double" || type.canonicalText == "java.lang.Float"
    }
}

private fun simpleTypes(): Set<String> = setOf(
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Short",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Float",
    "java.lang.Double",
    "java.lang.Boolean",
    "java.lang.Void",
    "java.lang.String"
)

object PsiSchemaUtils {
    fun generateSchema(type: PsiModifierListOwner?, project: Project): Schema<*> {

        val fieldName = PsiTypeUtils.getName(type) ?: ""

        val fieldDescription = PsiDocCommentUtils.getDocCommentTitle(type)

        val schemaFinal = PsiTypeUtils.getQualifiedName(type).let {
            if (it == "java.lang.Boolean") {
                BooleanSchema()
            } else if (it == "java.lang.String") {
                StringSchema()
            } else if (it == "java.lang.Byte") {
                IntegerSchema().format("int8")
            } else if (it == "java.lang.Short") {
                IntegerSchema().format("int16")
            } else if (it == "java.lang.Integer") {
                IntegerSchema().format("int32")
            } else if (it == "java.lang.Long") {
                IntegerSchema().format("int64")
            } else if (it == "java.lang.Float") {
                NumberSchema().format("float")
            } else if (it == "java.lang.Double") {
                NumberSchema().format("double")
            } else if (it == "java.util.Date") {
                StringSchema().format("date")
            } else if (PsiTypeUtils.isAssignedFrom(project, type, "java.util.List")) {
                //获取集合泛型
                ArraySchema().items(
                    // getParameters

                    generateSchema(
                        (type as PsiClassType).parameters[0] as PsiParameter, project
                    )
                )

            } else if (PsiTypeUtils.isAssignedFrom(project, type, "java.util.Map")) {
                MapSchema().additionalItems(
                    generateSchema(
                        (type as PsiClassType).parameters[1] as PsiParameter, project
                    )
                )
            } else {
                val clz = transToPsiClass(type)
                if (clz is PsiClass) {
                    val objectSchema = ObjectSchema()
                    for (field in clz.fields) {
                        //非static字段
                        if (
                                field.hasModifierProperty(PsiModifier.STATIC)
                                ||
                                field.hasModifierProperty(PsiModifier.FINAL)
                            ) {
                            continue
                        }

                        objectSchema.addProperty(field.name, generateSchema(field, project))
                    }
                    return objectSchema
                }
                throw Exception("未知类型:${type}")
            }
        }
        schemaFinal.description(fieldDescription).name(fieldName)
        return schemaFinal;
    }

    private fun transToPsiClass(type: PsiModifierListOwner?): PsiClass? {
        if (type is PsiClass) {
            return type
        }
        if (type is PsiParameter) {
            return (type.type as PsiClassType).resolve() as PsiClass
        }
        return null
    }
}
