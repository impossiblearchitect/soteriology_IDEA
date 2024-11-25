package dev.muridemo.soteriology.util

import dev.muridemo.soteriology.util.Nullable.*

import java.lang.invoke.{MethodHandle, MethodHandles}
import java.lang.reflect.{Constructor, Field, Method, Member, Type}
//import dev.muridemo.soteriology.util.cats.instances.given


//import scala.jdk.CollectionConverters.*

/**
 * A typeclass for members that can be represented by a `MethodHandle`.
 */
trait Handleable[-T <: Member] {
  def handle(t: T): Option[(String, Array[Type], Type, MethodHandle)]
}

given FieldHandle: Handleable[Field] with
  def handle(field: Field) = {
    val lookup = MethodHandles.lookup().nn
    field.trySetAccessible()
    Option.ofNullable(lookup.unreflectGetter(field))
          .map {(field.getName.nn, Array.empty, field.getType.nn, _)}
  }


given MethodHandle: Handleable[Method] with
  def handle(method: Method) = {
    val lookup = MethodHandles.lookup()
    method.trySetAccessible()
    Option.ofNullable(lookup.unreflect(method))
          .map {(method.getName, method.getGenericParameterTypes, method.getReturnType, _)}
  }


given ConstructorHandle: Handleable[Constructor[?]] with
  def handle(constructor: Constructor[?]) = {
    val lookup = MethodHandles.lookup()
    constructor.trySetAccessible()
    Option.ofNullable(lookup.unreflectConstructor(constructor))
          .map {(
            constructor.getName,
            constructor.getGenericParameterTypes,
            constructor.getDeclaringClass,
            _)}
  }

given GeneralizedFieldHandle: Handleable[Field | Method] with
  def handle(member: Field | Method) = member match {
    case field: Field => FieldHandle.handle(field)
    case method: Method => MethodHandle.handle(method)
  }


given UnifiedMemberHandle: Handleable[Field | Method | Constructor[?]] with
  def handle(member: Field | Method | Constructor[?]) = member match {
    case field: Field => FieldHandle.handle(field)
    case method: Method => MethodHandle.handle(method)
    case constructor: Constructor[?] => ConstructorHandle.handle(constructor)
  }

object MemberHandle {
  def unapply[T <: Member](t: T)(using handleable: Handleable[T]) = handleable.handle(t)
}


