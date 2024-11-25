package dev.muridemo.soteriology.util

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Constructor
import scala.reflect.ClassTag
import com.google.gson.JsonObject

import scala.jdk.CollectionConverters.*
import scala.collection.immutable.ArraySeq

import scala.util.boundary, boundary.{Label, break}
import scala.quoted.*

import dev.muridemo.soteriology.util.MemberHandle
import dev.muridemo.soteriology.util.Nullable.*

object Helpers {
  import nullable.?
  
  extension (inline opt: Option[Boolean])
    transparent inline def boolFlat = opt.contains(true)
  
  // enum Member[T] {
  //   case Field(field: Field)
  //   case Method(method: Method)
  //   case Constructor(constructor: Constructor[T])
  // }
  extension (obj: JsonObject)
    def getAsScalaArray(key: String) = {
      nullable:
        obj.getAsJsonArray(key).?.asList().nn.asScala.toArray
    }

    def getAsImmutableArray(key: String) = {
      nullable:
        ArraySeq.unsafeWrapArray(obj.getAsJsonArray(key).?.asList().nn.asScala.toArray)
    }

  extension (cls: Class[?])
    def getGeneralizedFields: Array[Field | Method] = {
      cls.getDeclaredFields.asInstanceOf[Array[Field | Method]] ++
        cls.getDeclaredMethods.nn.filter(_.nn.getParameterCount == 0).asInstanceOf[Array[Field | Method]]
    }

  
  // extension [T: FromExpr : ToExpr] (exprSeq: Expr[Seq[T]])
  //   def distribute(using Quotes): Seq[Expr[T]] = exprSeq.valueOrAbort.map(Expr(_)) 
  // case '{Seq[T](($hd +: ${tl : Expr[Seq[T]]})*)} => hd +: tl.distribute
  // case '{Seq.empty[T]} => Seq.empty
//  extension (sc: StringContext)
//    transparent inline def i(inline args: Any*) = ${Macros.inlineStringInterp('{sc.parts})('args)}
  // sc.s(args.map {
  //   case s: String => s
  //   case t => t.toString()
  // }*)

}
