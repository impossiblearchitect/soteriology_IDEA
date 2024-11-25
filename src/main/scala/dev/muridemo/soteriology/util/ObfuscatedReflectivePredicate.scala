package dev.muridemo.soteriology.util

import scala.reflect.ClassTag
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import dev.muridemo.soteriology.Soteriology
import dev.muridemo.soteriology.util.MemberHandle
import dev.muridemo.soteriology.util.Helpers.*
import dev.muridemo.soteriology.util.Nullable.*
//import dev.muridemo.soteriology.util.Nullable.nullable.{?, ??}

import net.neoforged.srgutils.IMappingFile.INode
import net.neoforged.srgutils.IMappingFile.IField
import net.neoforged.srgutils.IMappingFile.IMethod

class ObfuscatedReflectivePredicate[T](val memberName: String)(using ct: ClassTag[T]) extends (T => Boolean) {
  object Field:
    def unapply(name: String): Option[IField] = Option.ofNullable(classObfsMap.getField(name))
  object Method:
    def unapply(name: String): Option[IMethod] = Option.ofNullable(classObfsMap.getMethod(name, "()Z"))

  private val classObfsMap = {
    val key = ct.runtimeClass.getName.nn.replace('.', '/')
    Option.ofNullable(Soteriology.MAPPING.getClass(key))
      .orElse {
            val obfKey = Soteriology.MAPPING.remapClass(key)
            Option.ofNullable(Soteriology.MAPPING.getClass(obfKey))
      }.getOrElse {
        throw new IllegalArgumentException(s"No deobfuscation entry found for class $key")
      }
  }
  private val memberObfs = {
    nullable:
      (classObfsMap.getMethod(memberName, "()Z") ?? classObfsMap.getField(memberName)).?.getMapped()
  }
  private val memberHandle: MethodHandle = {
    val rc = ct.runtimeClass
    val lookup = MethodHandles.lookup()

    val (typ, handle) = rc.getGeneralizedFields.collectFirst{
      case MemberHandle(`memberObfs`, _, t, h) => (t, h)
    }.getOrElse{
      if memberObfs == null then
        throw new IllegalArgumentException(s"No deobfuscation entry found for member $memberName (assuming boolean type)")
      else
        throw new IllegalArgumentException(s"No field or method matching obfuscation $memberObfs of $memberName (mappings out of date?)")
    }
    handle.asType(MethodType.methodType(classOf[Boolean], rc)).!
  }

  override def apply(t: T): Boolean =
    try {
      val out = memberHandle.invoke(t).asInstanceOf[Boolean]
      out
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        false
    }
}
