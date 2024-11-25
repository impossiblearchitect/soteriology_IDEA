package dev.muridemo.soteriology.util.typelevel

import cats.Functor

import scala.reflect.ClassTag

object instances {

  given iSeqFunctor: Functor[IndexedSeq] with {
    def map[A, B](fa: IndexedSeq[A])(f: A => B): IndexedSeq[B] = fa.map(f)
  }
  
//  given arrayFunctor: Functor[Array] with {
//    def map[A, B](fa: Array[A])(f: A => B): Array[B] =
//      summon[ClassTag[B]]
//      fa.map(f)
//  }
}