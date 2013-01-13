package neurogenesis.doubleprecision
import neurogenesis.util.XMLOperator
import neurogenesis.util.Distribution
import neurogenesis.util.CComplexityMeasure
import scalala.library.random.MersenneTwisterFast
import scala.xml._

/*A simple input cell which can have both forward and recurrent connections
 * 
 */
class InCellD(fConns:NeuralConnsD,rConns:NeuralConnsD) extends EvolvableD {
  var stim = 0d
  var activation = 0d
  def activate(actFun: Function1[Double,Double]) : Double = {activation = actFun(stim); activation }
  def addRandomConnections(num:Int,rnd:MersenneTwisterFast) : Int = {
    fConns.addRandomConnections(num,rnd)
  }
  def burstMutate(prob:Double,dist:Distribution,rnd:MersenneTwisterFast) : InCellD = {
    val fc = fConns.burstMutate(prob,dist,rnd)
    var rc = rConns.burstMutate(prob,dist,rnd)
    new InCellD(fc,rc)
  }
  def burstMutate(prob:Double,dist:Distribution,rnd:MersenneTwisterFast,cpop:CellPopulationD) : InCellD = {
    val fc = fConns.burstMutate(prob,dist,rnd)
    var rc = rConns.burstMutate(prob,dist,rnd)
    val out2 = new InCellD(fc,rc)
    out2.setID(cpop.getCounter)
    cpop.add2Counter
    out2
  }
  def equals(other:InCellD) : Boolean = {
    (fConns == other.getForward && rConns == other.getRecurrent)
  }
  def getForward : NeuralConnsD = fConns
  def getRecurrent : NeuralConnsD = rConns
  def getActivation : Double = activation
  def gatherConnections : List[NeuralConnsD] = {
    List(fConns,rConns)
  }
  override def setFitness(f:Double,measure:ComplexityMeasure,cBias:Double) : Unit = {
    val c = measure.calculateComplexity(List(fConns,rConns),cBias)
    if (c != 0) {
      val fCand = f/c
      if (fCand > fitness) {
        fitness = fCand
      }
    }
    else {
      if (f > fitness) {
        fitness = f
      }
    }
  }
  def reset : Unit = { stim = 0; activation = 0 }
  def stimulate(s:Double) : Unit = { stim += s }
  
  def combine(e2: InCellD,dist:Distribution,mutP:Double,flipP:Double) : InCellD = {
    //val cops = implicitly[InCellD]
    val f = fConns.combine(e2.getForward,dist,mutP,flipP)
    val r = rConns.combine(e2.getRecurrent,dist,mutP,flipP)
    new InCellD(f,r)
  }
  def combine(e2: InCellD,dist:Distribution,mutP:Double,flipP:Double,rnd:MersenneTwisterFast,discardRate:Double=0.75) : InCellD = {
    //val cops = implicitly[InCellD]
    val f = fConns.combine(e2.getForward,dist,mutP,flipP,rnd,discardRate)
    val r = rConns.combine(e2.getRecurrent,dist,mutP,flipP,rnd,discardRate)
    new InCellD(f,r)
  }
  def combine(e2: InCellD,dist:Distribution,mutP:Double,flipP:Double,rnd:MersenneTwisterFast,discardRate:Double,cellpop:CellPopulationD) : InCellD = {
    //val cops = implicitly[InCellD]
    val f = fConns.combine(e2.getForward,dist,mutP,flipP,rnd,discardRate)
    val r = rConns.combine(e2.getRecurrent,dist,mutP,flipP,rnd,discardRate)
    val cell2 = new InCellD(f,r)
    cell2.setID(cellpop.getCounter)
    cellpop.add2Counter
    cell2
  }
  def complexify(in:Int,blocks:Int,memCells:Int,out:Int,addBlock:Boolean,rnd:MersenneTwisterFast) : InCellD = {
    new InCellD(fConns.complexify(in,blocks,memCells,out,addBlock,rnd),rConns.complexify(in,blocks,memCells,out,addBlock,rnd))
  }
  def distance(cell2:InCellD) : Double = {
    val d1 = fConns.dist(cell2.getForward)
    val d2 = rConns.dist(cell2.getRecurrent)
    d1+d2
  }
  def makeClone : InCellD = {
    val fw = getForward
    val rc = getRecurrent
    val nc = new InCellD(fw.makeClone,rc.makeClone)
    nc.setID(getID)
    nc
  }
  def toXML : Elem = {
    val fwd = <Forward>{fConns.toXML}</Forward>
    val rec = <Recurrent>{rConns.toXML}</Recurrent>
    val e = <InCellD>{fwd}{rec}</InCellD>
    e
  }

  
  override def toString : String = "<InCellD><Forward>"+fConns+"</Forward><Recurrent>\n"+rConns+"</Recurrent></InCellD>"
}
object InCellD {
  def fromXML(elem:Elem) : InCellD = {
    val fwd = elem \\ "Forward"
    val rec = elem \\ "Recurrent"
    val fc = new NeuralConnsD((fwd \\ "Min").text.toInt,(fwd \\ "Max").text.toInt)
    val rc = new NeuralConnsD((rec \\ "Min").text.toInt,(rec \\ "Max").text.toInt)
    val seq = XMLOperator.filterNodeSeq(fwd)
    for (s <- seq) {
      fc.addConnection((s \ "dest").text.toInt,(s \ "w").text.toDouble,(s \ "expr").text.toBoolean)
    }
    val seq2 = XMLOperator.filterNodeSeq(rec)
    for (s <- seq2) {
      rc.addConnection((s \ "dest").text.toInt,(s \ "w").text.toDouble,(s \ "expr").text.toBoolean)
    }
    new InCellD(fc,rc)
  }
  def fromXML(ns:NodeSeq) : InCellD = {
    val fwd = ns \\ "Forward"
    val fc = NeuralConnsD.fromXML(fwd)
    val rc = NeuralConnsD.fromXML(ns \\ "Recurrent")
    new InCellD(fc,rc)
  }
}