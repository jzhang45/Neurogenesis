package neurogenesis.doubleprecision

class TotalMeasure extends ComplexityMeasure {

  def calculateComplexity(allConns: List[NeuralConnsD], bias: Double): Double = {
    var numConns = 0
    var sumComplexity = 1.0
    for (c <- allConns) {
      numConns += c.conns.size
      sumComplexity += c.calculateComplexity
      }
    if (numConns > 0) {
      math.log(math.log(numConns)+bias+sumComplexity)
    }
    else {
      math.log(bias+sumComplexity)
    }
  }
  override def toString : String = "TotalMeasure"
  

}