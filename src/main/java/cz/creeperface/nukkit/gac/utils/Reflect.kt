package cz.creeperface.nukkit.gac.utils

import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

val finalAccess = if (System.getProperty("java.version").split('.')[0].toInt() <= 9) {
    val modifiersField = Field::class.java.getDeclaredField("modifiers")
    modifiersField.isAccessible = true

    { field: Field ->
        modifiersField.set(field, field.modifiers and Modifier.FINAL.inv())
    }
} else {
    val lookup = MethodHandles.privateLookupIn(Field::class.java, MethodHandles.lookup())
    val modifiers = lookup.findVarHandle(Field::class.java, "modifiers", Int::class.java)

    val action = { field: Field ->
        modifiers.set(field, field.modifiers and Modifier.FINAL.inv())
    }

    action
}

fun <T : Any> KClass<T>.accessProperty(name: String) = property(name).accessible()

fun <T : Any> KClass<T>.property(name: String): KProperty1<T, *> = this.declaredMemberProperties.find { it.name == name }!!

fun <T, K : KProperty<T>> K.accessible(): K {
    this.isAccessible = true
    return this
}

@Suppress("UNCHECKED_CAST")
fun <R, T, P, K : KProperty1<T, P>> K.getValue(receiver: T): R = this.get(receiver) as R

@Suppress("UNCHECKED_CAST")
fun <T, P, K : KProperty1<T, P>> K.setValue(receiver: T, value: Any?) {
    if (!this.isFinal && this is KMutableProperty<*>) {
        this.setter.call(receiver, value)
        return
    }

    val field = this.javaField!!
    field.isAccessible = true

    finalAccess(field)
    field.set(receiver, value)
}