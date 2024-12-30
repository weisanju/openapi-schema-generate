package com.weisanju.jsonschema

import com.google.gson.GsonBuilder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.*
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

private fun createProperty(
    type: PsiType,
    project: Project,
    propertyPath: MutableMap<PsiClass, ObjectProperty>,
    name: String?
): Property {

    val description = PsiDocCommentUtils.getDocCommentTitle(type)


    if (PsiTypeUtils.isArray(type, project)) {
        val property = ArrayProperty(description)
        val items: PsiType = if (PsiTypeUtils.isSimpleArray(type)) {
            (type as PsiArrayType).componentType
        } else {
            ((type as PsiClassType).parameters[0] as PsiClassType)
        }

        val createProperty = createProperty(items, project, propertyPath, null)
        if (name != null && createProperty.name == null) {
            if (name.endsWith("s")) {
                createProperty.name = name.dropLast(1)
            } else if (name.endsWith("List")) {
                createProperty.name = name.dropLast(4)
            } else if (name.endsWith("Array")) {
                createProperty.name = name.dropLast(5)
            } else {
                createProperty.name = name
            }
        }
        property.items = createProperty
        return property
    }


    if (PsiTypeUtils.isSimpleType(type)) {

        val property: Property;
        if (PsiTypeUtils.isInteger(type)) {
            property = IntegerProperty(description)
        } else if (PsiTypeUtils.isBoolean(type)) {
            property = BooleanProperty(description)
        } else if (PsiTypeUtils.isNumber(type)) {
            property = NumberProperty(description)
        } else {
            property = StringProperty(description)
        }
        property.name = name
        return property
    }

    //map类型
    if (PsiTypeUtils.isMap(project, type)) {
        //获取 Map 的 key Value
        val obj = ObjectProperty(description)
        obj.additionalProperties = createProperty(PsiTypeUtils.getMapValueType(type), project, propertyPath, null)
        obj.name = name
        return obj
    }


    if (PsiTypeUtils.isJdkBuildIn(type)) {
        return StringProperty(description)
    }


    val clz = (type as PsiClassType).resolve()!!

    if (propertyPath.containsKey(clz)) {
        val property = propertyPath[clz]!!

        val objectProperty = ObjectProperty(property)
        objectProperty.name = name
        objectProperty.properties = mutableMapOf();
        objectProperty.additionalProperties = null;
        return objectProperty
    }

    val obj = ObjectProperty(description)

    //防止循环引用。 生成过的类不再生成
    propertyPath[clz] = obj

    clz.allFields.forEach {
        //判断是否是静态字段 或者 final字段
        if (it.hasModifierProperty(PsiModifier.STATIC) || it.hasModifierProperty(PsiModifier.FINAL)) {
            return@forEach
        }

        //忽略 注释中 带有 @ignore 的字段
        if (PsiDocCommentUtils.hasIgnoreTag(it)) {
            return@forEach
        }

        val prop = createProperty(it.type, project, propertyPath, it.name)
        prop.description = PsiDocCommentUtils.getDocCommentTitle(it) ?: it.name
        prop.example = PsiDocCommentUtils.getTagText(it, "example")
        prop.required = PsiAnnotationUtils.annotationExists("javax.validation.constraints.NotNull", it)
        prop.name = it.name
        obj.properties[it.name] = prop
    }
    obj.name = name
    return obj
}


class ApiPostDomainGenerate : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {

        val obj = doExtractApiSchema(event)

        val gson = GsonBuilder().setPrettyPrinting().create()

        val jsonString = gson.toJson(obj)

        ClipboardUtil.copyToClipboard(jsonString)
    }

    companion object {
        fun doExtractApiSchema(event: AnActionEvent): Property {
            val project = event.project!!

            //获取选中的类、选中的方法
            val (psiClass) = PsiUtils.getSelected(event)
            val obj = createProperty(PsiTypeUtils.getPsiTypeFromPsiClass(psiClass), project, mutableMapOf(), null)
            return obj
        }

    }


}


class MarkDownGenerate : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val extractApiSchema = ApiPostDomainGenerate.doExtractApiSchema(event)
        ClipboardUtil.copyToClipboard(writeMarkdown(extractApiSchema))
    }
}