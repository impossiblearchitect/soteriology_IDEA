package dev.muridemo.soteriology.util.typelevel

import cats.arrow.FunctionK
import cats.{Distributive, Foldable, Functor, MonoidK}

import scala.reflect.ClassTag

object instances {

  given iSeqFunctor: Functor[IndexedSeq] with
    def map[A, B](fa: IndexedSeq[A])(f: A => B): IndexedSeq[B] = fa.map(f)


//  given Distributive[Option] with
//    def distribute[G[_] : Functor, A, B](ga: G[A])(f: A => Option[B]) = {
//
//    }

//  given arrayFunctor: Functor[Array] with {
//    def map[A, B](fa: Array[A])(f: A => B): Array[B] =
//      summon[ClassTag[B]]
//      fa.map(f)
//  }
}