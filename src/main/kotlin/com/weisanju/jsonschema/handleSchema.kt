package com.weisanju.jsonschema

fun handleSchema(fieldName: String, schema: Property, schemasQueue: MutableList<Property>, writer: StringBuilder) {
    val description = schema.description ?: ""

    val isRequired = if (schema.required == true) "是" else "否"

    val example = schema.example ?: ""

    //判断类型
    when (schema) {
        is ObjectProperty -> {
            val additionalProperties = schema.additionalProperties

            if (additionalProperties != null) {
                val valueType = additionalProperties.type ?: ""

                if (valueType == "object" || valueType == "array") {
                    schemasQueue.add(additionalProperties)
                }

                writer.appendLine("| $fieldName | map\\<string,$valueType> | $description | $isRequired | $example |")
            } else {
                writer.appendLine("| $fieldName | ${capitalize(fieldName)} | $description | $isRequired | $example |")
                schemasQueue.add(schema)
            }
        }

        is ArrayProperty -> {
            val items = schema.items!!

            var itemType = items.type ?: ""

            val rawItemType = itemType

            //组装ItemType
            if (itemType == "object") {
                itemType = capitalize(fieldName)
                if (itemType.endsWith("s")) {
                    itemType = itemType.dropLast(1)
                }
            }

            writer.appendLine("| $fieldName | array\\<$itemType> | $description | $isRequired | $example |")

            if (rawItemType == "object" || rawItemType == "array") {
                //items 命名规则 文件名称夹
                schemasQueue.add(items)
            }
        }

        else -> {
            writer.appendLine("| $fieldName | ${schema.type} | $description | $isRequired | $example |")
        }
    }
}

fun capitalize(s: String): String {
    return s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun main() {
    val root: Property = ObjectProperty()

    writeMarkdown(root)
}

fun writeMarkdown(root: Property): String {
    val schemasQueue: MutableList<Property> = mutableListOf(root)

    //kotlin 字符串拼接
    val writer = StringBuilder()


    while (schemasQueue.isNotEmpty()) {

        val schema = schemasQueue.removeAt(0)

        val fieldName = schema.name ?: ""

        var description = schema.description ?: ""

        if (fieldName.isNotEmpty() && description.isNotEmpty()) {
            description = "($description)"
        }

        writer.appendLine("\n### ${capitalize(fieldName)}$description")
        writer.appendLine("\n| 字段名称 | 字段类型 | 字段描述 | 是否必填 | 示例值 |")
        writer.appendLine("| --- | --- | --- | --- | --- |")

        if (schema is ObjectProperty) {
            for (property in schema.properties) {
                handleSchema(property.key, property.value, schemasQueue, writer)
            }
        } else if (schema is ArrayProperty) {
            handleSchema(schema.name ?: "", schema.items!!, schemasQueue, writer)
        }
    }

    return writer.toString()
}
