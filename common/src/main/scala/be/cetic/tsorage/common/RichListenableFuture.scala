// Source:
// - https://stackoverflow.com/questions/18026601/listenablefuture-to-scala-future
// - https://github.com/prad-a-RuntimeException/semantic-store/blob/master/src/main/scala/recipestore/misc/RichListenableFuture.scala

package be.cetic.tsorage.common

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}

// Convert a ListenableFuture (Java) to a Future (Scala).
object RichListenableFuture {

  implicit class RichListenableFuture[T](lf: ListenableFuture[T]) {
    def asScala: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(lf, new FutureCallback[T] {
        def onFailure(t: Throwable): Unit = p failure t

        def onSuccess(result: T): Unit = p success result
      })
      p.future
    }
  }

}
