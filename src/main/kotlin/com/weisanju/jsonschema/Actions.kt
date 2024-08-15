package com.weisanju.jsonschema

import com.google.gson.GsonBuilder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.parameters.RequestBody


class OpenApiSchemaGenerate : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project!!

        val (psiClass, selectMethod) = PsiUtils.getSelected(event)

        val requestPath1 = PsiAnnotationUtils.getAnnotationValue(
            "org.springframework.web.bind.annotation.RequestMapping",
            "value",
            psiClass
        )

        val requestPath2 = PsiAnnotationUtils.getAnnotationValue(
            "org.springframework.web.bind.annotation.RequestMapping",
            "value",
            selectMethod!!
        )

        //判断 GET POST DELETE

        val annotation =
            PsiAnnotationUtils.getAnnotation("org.springframework.web.bind.annotation.RequestMapping", selectMethod)

        val pathItem = PathItem()
        pathItem.summary = PsiDocCommentUtils.getDocCommentTitle(selectMethod)

        val op = Operation()

        (annotation["method"] ?: "RequestMethod.GET").let {
            when (it) {
                "RequestMethod.GET" -> pathItem.get = op
                "RequestMethod.POST" -> pathItem.post = op
                "RequestMethod.DELETE" -> pathItem.delete = op
                "RequestMethod.PUT" -> pathItem.put = op
                "RequestMethod.HEAD" -> pathItem.head = op
            }
        }

        op.summary = pathItem.summary
        op.operationId = requestPath2

        op.parameters = ArrayList()

        //判断所有参数
        for (parameter in selectMethod.parameterList.parameters) {

            val requestBody = PsiAnnotationUtils.annotationExists(
                "org.springframework.web.bind.annotation.RequestBody",
                parameter
            )

            if (requestBody) {
                val schema = PsiSchemaUtils.generateSchema(parameter, project)
                op.requestBody(
                    RequestBody().required(true).content(
                        Content().addMediaType(
                            "application/json",
                            io.swagger.v3.oas.models.media.MediaType().schema(schema)
                        )
                    )
                );
                continue
            }

            //判断是否是简单类型
            if (PsiTypeUtils.isSimpleType(parameter.type)) {
            }
            //简单数组类型
            else if (PsiTypeUtils.isSimpleArray(parameter.type)) {
            }
            //集合类型
            else if (PsiTypeUtils.isCollection(parameter.type, project)) {

            }
            //其他类型
            else {

            }
        }
        println(op)
    }
}


class ApiPostDomainGenerate : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {

        val project = event.project!!

        //获取选中的类、选中的方法

        val (psiClass) = PsiUtils.getSelected(event)

        val obj = createProperty(PsiTypeUtils.getPsiTypeFromPsiClass(psiClass), project)

        val gson = GsonBuilder().setPrettyPrinting().create()

        val jsonString = gson.toJson(obj)


        ClipboardUtil.copyToClipboard(jsonString)

    }

    private fun createProperty(
        type: PsiType,
        project: Project
    ): Property {
        val description = PsiDocCommentUtils.getDocCommentTitle(type)

        if (PsiTypeUtils.isArray(type, project)) {
            val property = ArrayProperty(description)
            val items: PsiType = if (PsiTypeUtils.isSimpleArray(type)) {
                (type as PsiArrayType).componentType
            } else {
                ((type as PsiClassType).parameters[0] as PsiClassType)
            }

            property.items = createProperty(items, project)

            return property
        }


        if (PsiTypeUtils.isSimpleType(type)) {

            var property: Property;
            if (PsiTypeUtils.isInteger(type)) {
                property = IntegerProperty(description)
            } else if (PsiTypeUtils.isBoolean(type)) {
                property = BooleanProperty(description)
            } else if (PsiTypeUtils.isNumber(type)) {
                property = NumberProperty(description)
            } else {
                property = StringProperty(description)
            }
            return property
        }


        if (PsiTypeUtils.isJdkBuildIn(type)) {
            return StringProperty(description)
        }


        val obj = ObjectProperty(description)

        val clz = (type as PsiClassType).resolve()

        clz?.allFields?.forEach {

            //判断是否是静态字段 或者 final字段
            if (it.hasModifierProperty(PsiModifier.STATIC) || it.hasModifierProperty(PsiModifier.FINAL)) {
                return@forEach
            }

            val prop = createProperty(it.type, project)

            prop.description = PsiDocCommentUtils.getDocCommentTitle(it) ?: it.name

            obj.properties[it.name] = prop
        }

        return obj
    }

}