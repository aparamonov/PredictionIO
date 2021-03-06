package io.prediction.controller

import org.scalatest.FunSuite
import org.scalatest.Inside
import org.scalatest.Matchers._
import org.scalatest.Inspectors._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import _root_.java.lang.Thread

import io.prediction.controller._
import io.prediction.core._
import io.prediction.workflow.SharedSparkContext
//import io.prediction.workflow.WorkflowParams
import grizzled.slf4j.{ Logger, Logging }


import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite

object TestEvaluator {
  case class EvalInfo(val id: Int, val ex: Int) 
  case class Query(val id: Int, val ex: Int, val qx: Int) 
  case class Prediction(val id: Int, val ex: Int, val qx: Int)
  case class Actual(val id: Int, val ex: Int, val qx: Int)

  class FakeEngine(val id: Int, val en: Int, val qn: Int)
  extends BaseEngine[EvalInfo, Query, Prediction, Actual] {
    def train(
      sc: SparkContext, 
      engineParams: EngineParams,
      instanceId: String = "",
      params: WorkflowParams = WorkflowParams()
    ): Seq[Any] = {
      Seq[Any]()
    }

    def eval(sc: SparkContext, engineParams: EngineParams)
    : Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = {
      (0 until en).map { ex => {
        val qpas = (0 until qn).map { qx => {
          (Query(id, ex, qx), Prediction(id, ex, qx), Actual(id, ex, qx))
        }}
  
        (EvalInfo(id = id, ex = ex), sc.parallelize(qpas))
      }}
    }
  
  }

  class Evaluator0 extends Evaluator[EvalInfo, Query, Prediction, Actual,
      (Query, Prediction, Actual), 
      (EvalInfo, Seq[(Query, Prediction, Actual)]),
      Seq[(EvalInfo, (EvalInfo, Seq[(Query, Prediction, Actual)]))]
      ] {

    def evaluateUnit(q: Query, p: Prediction, a: Actual)
    : (Query, Prediction, Actual) = (q, p, a)

    def evaluateSet(
        evalInfo: EvalInfo, 
        eus: Seq[(Query, Prediction, Actual)])
    : (EvalInfo, Seq[(Query, Prediction, Actual)]) = (evalInfo, eus)

    def evaluateAll(
      input: Seq[(EvalInfo, (EvalInfo, Seq[(Query, Prediction, Actual)]))]) 
    = input
  }
}



class EvaluatorSuite
extends FunSuite with Inside with SharedSparkContext {
  import io.prediction.controller.TestEvaluator._
  @transient lazy val logger = Logger[this.type] 

  test("Evaluator.evaluate") {
    val engine = new FakeEngine(1, 3, 10)
    val evaluator = new Evaluator0()
  
    val evalDataSet = engine.eval(sc, null.asInstanceOf[EngineParams])
    val er: Seq[(EvalInfo, (EvalInfo, Seq[(Query, Prediction, Actual)]))] =
      evaluator.evaluateBase(sc, evalDataSet)

    evalDataSet.zip(er).map { case (input, output) => {
      val (inputEvalInfo, inputQpaRDD) = input
      val (outputEvalInfo, (outputEvalInfo2, outputQpaSeq)) = output
      
      inputEvalInfo shouldBe outputEvalInfo
      inputEvalInfo shouldBe outputEvalInfo2
      
      val inputQpaSeq: Array[(Query, Prediction, Actual)] = inputQpaRDD.collect

      inputQpaSeq.size should be (outputQpaSeq.size)
      // TODO. match inputQpa and outputQpa content.
    }}
  }
}
