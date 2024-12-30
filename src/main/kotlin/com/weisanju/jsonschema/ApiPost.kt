package com.weisanju.jsonschema

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.Serializable

open class Property(
    var description: String? = null,
    var type: String? = null,
    var required: Boolean? = null,
    var example: String? = null,
    var name: String? = null
) :
    Serializable {
}

open class ObjectProperty() : Property(type = "object") {
    var properties: MutableMap<String, Property> = mutableMapOf();

    var additionalProperties: Property? = null;

    constructor(description: String? = null) : this() {
        this.description = description
    }
}

class ArrayProperty() : Property(type = "array") {
    var items: Property? = null

    constructor(description: String? = null) : this() {
        this.description = description
    }
}


class StringProperty() : Property(type = "string") {
    constructor(description: String? = null) : this() {
        this.description = description
    }
}


class IntegerProperty() : Property(type = "integer") {
    constructor(description: String? = null) : this() {
        this.description = description
    }
}


class NumberProperty() : Property(type = "number") {
    constructor(description: String? = null) : this() {
        this.description = description
    }
}


class BooleanProperty() : Property(type = "boolean") {
    constructor(description: String? = null) : this() {
        this.description = description
    }
}

fun main() {


    val objectProperty = ObjectProperty("test")
    //to json string

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    val jsonString = gson.toJson(gson)

    println(jsonString)
}



