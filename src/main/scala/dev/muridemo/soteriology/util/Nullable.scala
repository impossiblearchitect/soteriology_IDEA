package dev.muridemo.soteriology.util

import _root_.cats.{Functor, Id}
import _root_.cats.syntax.all.*
import typelevel.instances.given
import dev.muridemo.soteriology.util

import javax.annotation.Nonnull
import scala.quoted.{Expr, Quotes}
import scala.util.{TupledFunction, boundary}
import scala.util.boundary.{Label, break}

object Nullable:
  extension [T](r: T | Null)
    /** <p>An alias for `.nn`.</p>
     * <p>Documents intent that null values should throw an NPE,
     * allowing `.nn` to be reserved for the case where null values
     * should never occur to begin with (e.g. in places where `@NotNull` is inferrable).</p>*/
    //Implementation note: is not inline so that it shows up in the stack trace as itself and not `.nn`.
    def ! : T = {r.nn}
//  type Nonnull[T] = T match
//    case |[s, Null] => s
//    case Null => Nothing
//    case _ => T

//  object Nonnull:
//    def apply[T](t: T): Nonnull[T] = t.nn
//    def apply_![T](t: T): Nonnull[T] = t.!

  extension (Opt: Option.type)
    def ofNullable[T](nullable: T | Null): Option[T] =
      scala.Option(nullable).map(_.nn)
      
  extension [T](inline opt:Option[T])
    inline def mapNullable[R](f: T => R | Null) : Option[R] = {
      opt.flatMap(x => Option.ofNullable(nullable = f(x)))
    }

//  extension [F, A <: Tuple, B](inline f: F)
//    inline def apply_nn[FN](using tf: TupledFunction[F, A => B], tfn: TupledFunction[FN, Tuple.Map[A, [T] =>> (T | Null)] => B]) = {
//      // Scalac isn't strong enough to prove that `Nonnull[T | Null] =:= T` for arbitrary `T`,
//      // let alone that Map[Map[A, Nullable], Nonnull] =:= A. 
//      // So, we cheat with a cast.
//      val out = (a: Tuple.Map[A, [T] =>> (T | Null)]) => 
//        tf.tupled(f)(a.map([T] => (t: T) => Id(t)).asInstanceOf[A])
//      tfn.untupled(out)
//    }
//    inline def apply_![FN](using tf: TupledFunction[F, A => B], tfn: TupledFunction[FN, Tuple.Map[A, [T] =>> (T | Null)] => B]) = {
//      val out = (a: Tuple.Map[A, [T] =>> (T | Null)]) =>
//        tf.tupled(f)(a.map([T] => (t: T) => Id(t)).asInstanceOf[A])
//      tfn.untupled(out)
//    }

  extension [F[_] : Functor, T](inline c: F[T | Null] | Null)
    transparent inline def all_nn = c.nn.map(_.nn)

  object nullable:
    inline def apply[T](inline body: Label[Null] ?=> T): T | Null =
      boundary(body)


    extension [T](r: T | Null)
      /** Exits with `null` to next enclosing `nullable` boundary */
      transparent inline def ? (using Label[Null]): T =
        if r == null then break(null) else r.asInstanceOf[T]
      /** Tests `r`, returning `alt` if `null`.
       * Does *not* provide a `nullable` context for `r` (use `??:` instead).*/
      transparent inline def ?? (inline alt: => T) =
        if r == null then alt else r
      /** Tests `r`, throwing `alt` if `null`.
       * Does *not* provide a `nullable` context for `r` (use `??:` instead).*/
      transparent inline def ??![E <: Exception] (inline alt: => E) : T =
        if r == null then throw alt else r.asInstanceOf[T]
    extension [T](inline r: Label[Null] ?=> T)
      /** Tests `r` in a `nullable` context, returning `alt` if `null` */
      transparent inline def ??: (inline alt: => T): T =
        nullable(r) match
          case null => alt
          case nn: T => nn
//        val eval = nullable(r)
//        if eval == null then alt else eval

  export nullable.{?, ??, ??!, ??:}