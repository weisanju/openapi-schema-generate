package com.weisanju.jsonschema

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody


class HelloWorldAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val message = "Hello,World!"
        // Show dialog with message
        Messages.showMessageDialog(project, message, "Greeting", Messages.getInformationIcon())

        val openAPI = OpenAPI()

        openAPI.info = generateInfo();

        val paths = Paths()


        paths.addPathItem("", PathItem())

        openAPI.paths = paths;
    }

    private fun generateInfo(): Info {
        val info = Info()
        info.title = ""
        info.summary = ""
        info.description = ""
        return info
    }
}

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
            selectMethod
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
                val schema = PsiSchemaUtils.generateSchema(parameter.type, project)
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

    private fun getFieldName(field: PsiField): String {
        val fieldAnnotation = PsiAnnotationUtils.getAnnotation("com.fasterxml.jackson.annotation.JsonProperty", field)
        val fieldName = fieldAnnotation["value"] ?: field.name
        return fieldName
    }
}