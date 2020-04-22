package io.chrisdavenport.epimetheus.redis4cats

import cats._
import cats.implicits._
import cats.effect._
import io.chrisdavenport.epimetheus._
import shapeless._


trait RedisMetricOps[M[_]] {
  /**
    * Increases the count of active requests
    *
    * @param classifier the classifier to apply
    */
  def active(classifier: Option[String]): Resource[M, Unit]

  /**
    * Records the time to fully consume the response, including the body
    *
    * @param elapsed the time to record
    * @param classifier the classifier to apply
    */
  def recordTotalTime(
    terminationType: RedisMetricOps.TerminationType,
    elapsed: Long,
    classifier: Option[String]
  ): M[Unit]

}

object RedisMetricOps {

  def build[F[_]: Sync](
      cr: CollectorRegistry[F],
      prefix: Name = Name("redis4cats"), 
      buckets: List[Double] = Histogram.defaults,
    ): F[RedisMetricOps[F]] = 
      MetricsCollection.build[F](cr, prefix, buckets).map(new EpOps(_))

  sealed trait TerminationType

  object TerminationType {

    /** Signals Succesful Completion **/
    case object Success extends TerminationType

    /** Signals cancelation */
    case object Canceled extends TerminationType

    /** Signals an abnormal termination due to an error processing the request, either at the server or client side */
    case class Error(rootCause: Throwable) extends TerminationType

    /** Signals a client timing out during a request */
    case object Timeout extends TerminationType
  }


  private final case class Classifier(value: String) extends AnyVal
  private object Classifier {
    def opt(x: Option[String]): Classifier = Classifier(x.getOrElse(""))
  }
  

  def reportTermination(t: TerminationType): String = t match {
    case TerminationType.Success => "success"
    case TerminationType.Error(_) => "error"
    case TerminationType.Timeout => "timeout"
    case TerminationType.Canceled => "canceled"
  }

  private case class MetricsCollection[M[_]](
    operations: UnlabelledHistogram[M, (TerminationType, Classifier)],
    active: UnlabelledGauge[M, Classifier]
  )
  private object MetricsCollection {
    def build[F[_]: Sync](
      cr: CollectorRegistry[F],
      prefix: Name, 
      buckets: List[Double],
    ) = for {
      operations <- Histogram.labelledBuckets(
        cr,
        prefix |+| Name("_") |+| Name("operation_terminations"),
        "Total Operations.",
        Sized(Label("termination_type"), Label("classifier")),
        { op: (TerminationType, Classifier) => Sized(reportTermination(op._1), op._2.value)},
        buckets:_*
      )
      active<- Gauge.labelled(
        cr, 
        prefix |+| Name("_") |+| Name("active_request_count"),
        "Total Active Requests.",
        Sized(Label("classifier")),
        {c: Classifier => Sized(c.value)}
      )

    } yield MetricsCollection[F](operations, active)
  }

  private class EpOps[F[_]: Functor](metricsCollection: MetricsCollection[F]) extends RedisMetricOps[F]{
    def active(classifier: Option[String]): Resource[F,Unit] = {
      val active = metricsCollection.active.label(Classifier.opt(classifier))
      Resource.make(active.inc)(_ => active.dec)
    }

    def recordTotalTime(terminationType: TerminationType, elapsed: Long, classifier: Option[String]): F[Unit] = {
      metricsCollection.operations.label((terminationType, Classifier.opt(classifier)))
        .observe(elapsed.toDouble)
    }
  }

}