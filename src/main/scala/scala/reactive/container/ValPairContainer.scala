package scala.reactive
package container






trait ValPairContainer[@spec(Int, Long, Double) P, @spec(Int, Long, Double) Q] {
  private[reactive] var liftedContainer: ValPairContainer.Lifted[P, Q] = _

  def init(dummy: ValPairContainer[P, Q]) {
    liftedContainer = new ValPairContainer.Lifted[P, Q](this)
  }

  init(this)

  def react = liftedContainer

  def inserts: RValPair[P, Q]

  def removes: RValPair[P, Q]

  def filter1(p: P => Boolean): ValPairContainer[P, Q] = {
    new ValPairContainer.Filter1(this, p)
  }

  def filter2(p: Q => Boolean): ValPairContainer[P, Q] = {
    new ValPairContainer.Filter2(this, p)
  }

  def swap: ValPairContainer[Q, P] = {
    new ValPairContainer.Swap(this)
  }

}


object ValPairContainer {

  class Lifted[@spec(Int, Long, Double) P, @spec(Int, Long, Double) Q](val container: ValPairContainer[P, Q]) {
    def to[That <: RHashValMap[P, Q]](implicit factory: ValPairBuilder.Factory[P, Q, That]): That = {
      val builder = factory()
      val result = builder.container
  
      result.subscriptions += container.inserts.mutate(builder) {
        pair => builder.insertPair(pair._1, pair._2)
      }
      result.subscriptions += container.removes.mutate(builder) {
        pair => builder.removePair(pair._1, pair._2)
      }
  
      result
    }
    def mutate(m: ReactMutable)(insert: RValPair.Signal[P, Q] => Unit)(remove: RValPair.Signal[P, Q] => Unit): Events.Subscription = {
      new Mutate(container, m, insert, remove)
    }
  }

  class Emitter[@spec(Int, Long, Double) P, @spec(Int, Long, Double) Q]
  extends ValPairContainer[P, Q] {
    private[reactive] var insertsEmitter: RValPair.Emitter[P, Q] = _
    private[reactive] var removesEmitter: RValPair.Emitter[P, Q] = _

    def init(dummy: Emitter[P, Q]) {
      insertsEmitter = new RValPair.Emitter[P, Q]
      removesEmitter = new RValPair.Emitter[P, Q]
    }

    init(this)

    def inserts = insertsEmitter

    def removes = removesEmitter
  }

  class Filter1[@spec(Int, Long, Double) P, @spec(Int, Long, Double) Q]
    (val container: ValPairContainer[P, Q], p: P => Boolean)
  extends ValPairContainer[P, Q] {
    val inserts = container.inserts.filter1(p)
    val removes = container.removes.filter1(p)
  }

  class Filter2[@spec(Int, Long, Double) P, @spec(Int, Long, Double) Q]
    (val container: ValPairContainer[P, Q], p: Q => Boolean)
  extends ValPairContainer[P, Q] {
    val inserts = container.inserts.filter2(p)
    val removes = container.removes.filter2(p)
  }

  class Swap[@spec(Int, Long, Double) P, @spec(Int, Long, Double) Q]
    (val container: ValPairContainer[P, Q])
  extends ValPairContainer[Q, P] {
    val inserts = container.inserts.swap
    val removes = container.removes.swap
  }

  class Mutate[@spec(Int, Long, Double) P, @spec(Int, Long, Double) Q, M <: ReactMutable]
    (val container: ValPairContainer[P, Q], val m: M, val ins: RValPair.Signal[P, Q] => Unit, val rem: RValPair.Signal[P, Q] => Unit)
  extends Events.ProxySubscription {
    val subscription = Events.CompositeSubscription(
      container.inserts.mutate(m)(ins),
      container.removes.mutate(m)(rem)
    )
  }

}
