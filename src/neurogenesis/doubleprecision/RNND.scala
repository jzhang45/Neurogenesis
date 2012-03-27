package neurogenesis.doubleprecision

import scala.util.Random
import scala.xml.Elem
import scala.xml.TopScope
import scalala.tensor.dense.DenseMatrix
import scalala.tensor.mutable.Matrix
import edu.uci.ics.jung.graph.SparseGraph
import scalala.library.LinearAlgebra
import edu.uci.ics.jung.graph.util.Pair
import edu.uci.ics.jung.graph.util.EdgeType

class RNND(inputLayer:Array[InCellD],cellBlocks:Array[CellBlockD],outputLayer:Array[OutCellD]) extends EvolvableD {
  val in = inputLayer.length
  val numBlocks = cellBlocks.length
  val blockSize = cellBlocks(0).getSize - 3 //
  val out = outputLayer.length
  var firstInput = true
  val memGates = cellBlocks(0).getSize //number of inputs the blocks have 
  val midPoint = in + numBlocks*memGates //after these only output cells remain
  
  
  def activate(inputs:Array[Double],actFun:Function1[Double,Double]) : Array[Double] = {
	  val res = new Array[Double](outputLayer.size)
	  if (!firstInput) {
		for (i <- 0.until(inputs.length)) {
	      stimulate(inputLayer(i).getActivation,inputLayer(i).getRecurrent)
	    }
		for (i <- 0.until(numBlocks)) {
		  val acts = cellBlocks(i).activate
		  for (j <- 0.until(acts.length)) {
		    stimulate(acts(j),cellBlocks(i).getRecurrent(j))
		  }
		}
		for (i <- 0.until(out)) {
		  stimulate(outputLayer(i).getActivation,outputLayer(i).getRecurrent)
		}
	  }
	  val actInput = new Array[Double](inputs.length)
	  for (i <- 0.until(inputs.length)) {
	    inputLayer(i).stimulate(inputs(i))
	    actInput(i) = inputLayer(i).activate(actFun)
	    stimulate(actInput(i),inputLayer(i).getForward)
	  }
	  for (i <- 0.until(numBlocks)) {
	    val actBlock = cellBlocks(i).activate
	    for (j <- 0.until(actBlock.length)) {
	      stimulate(actBlock(j),cellBlocks(i).getForward(j))
	    }
	  }
	  for (i <- 0.until(out)) {
	    res(i) = outputLayer(i).activate(actFun)
	  }
	  firstInput = false
	  res
  }
  /**Adds recurrent stimulations to the network
   * 
   */
  def recStim : Unit = {
    for (i <- 0.until(in)) {
	  stimulate(inputLayer(i).getActivation,inputLayer(i).getRecurrent)
    }
	for (i <- 0.until(numBlocks)) {
	  val acts = cellBlocks(i).activate
	  for (j <- 0.until(acts.length)) {
		stimulate(acts(j),cellBlocks(i).getRecurrent(j))
	  }
	}
	for (i <- 0.until(out)) {
	  stimulate(outputLayer(i).getActivation,outputLayer(i).getRecurrent)
	}
  }
  def evolinoActivate(inputs:Array[Double],actFun:Function1[Double,Double]) : Array[Double] = {
	  val res = new Array[Double](outputLayer.size+numBlocks*blockSize)
	  if (!firstInput) {
		recStim
	  }
	  val actInput = new Array[Double](inputs.length)
	  for (i <- 0.until(inputs.length)) {
	    inputLayer(i).stimulate(inputs(i))
	    actInput(i) = inputLayer(i).activate(actFun)
	    stimulate(actInput(i),inputLayer(i).getForward)
	  }
	  for (i <- 0.until(numBlocks)) {
	    val actBlock = cellBlocks(i).activate
	    for (j <- 0.until(actBlock.length)) {
	      stimulate(actBlock(j),cellBlocks(i).getForward(j))
	    }
	  }
	  for (i <- 0.until(out)) {
	    res(i) = outputLayer(i).activate(actFun)
	  }
	  for (i <- 0 until numBlocks) {
        for (j <- 0 until blockSize) {
          res(out+i*blockSize+j) = cellBlocks(i).memState(j)
        }
      }
	  firstInput = false
	  res
  }
	/*
	def getNet(inCells:Array[InCell],blocks:Array[CellBlock],outCells:Array[OutCell]) : RNN = {
	  val rnn = new RNN(inCells.length,blocks.length,outCells.length)
	  for (i <- 0.until(inCells.length)) {
	    rnn.inputLayer(i) = inCells(i)
	  }
	  for (i <- 0.until(blocks.length)) {
	    rnn.cellBlocks(i) = blocks(i)
	  }
	  for (i <- 0.until(outCells.length)) {
	    rnn.outputLayer(i) = outCells(i)
	  }
	  rnn
	}
	*/
  def burstMutate(prob:Double,dist:Distribution,rnd:Random) : RNND = {
    val il = new Array[InCellD](in)
    for (i <- 0 until in) {
      il(i) = inputLayer(i).burstMutate(prob,dist,rnd)
    }
    val bl = new Array[CellBlockD](numBlocks)
    for (i <- 0 until numBlocks) {
      bl(i) = cellBlocks(i).burstMutate(prob,dist,rnd)
    }
    val ol = new Array[OutCellD](out)
    for (i <- 0 until out) {
      ol(i) = outputLayer(i).burstMutate(prob,dist,rnd)
    }
    new RNND(il,bl,ol)
  }
  def getIn(idx:Int) : InCellD = inputLayer(idx)
  def getMid(idx:Int) : CellBlockD = cellBlocks(idx)
  def getOut(idx:Int) : OutCellD = outputLayer(idx)
  
  def combine(net2:RNND,dist:Distribution,mutP:Double,flipP:Double) : RNND = {
    val il = new Array[InCellD](in)
    val bl = new Array[CellBlockD](numBlocks)
    val ol = new Array[OutCellD](out)
    for (i <- 0 until in) {
      il(i) = inputLayer(i).combine(net2.getIn(i),dist,mutP,flipP)
    }
    for (i <- 0 until numBlocks) {
      bl(i) = cellBlocks(i).combine(net2.getMid(i),dist,mutP,flipP)
    }
    for (i <- 0 until out) {
      ol(i) = outputLayer(i).combine(net2.getOut(i),dist,mutP,flipP)
    }
    new RNND(il,bl,ol)
  }
  def combine(net2:RNND,dist:Distribution,mutP:Double,flipP:Double,rnd:Random) : RNND = {
    val il = new Array[InCellD](in)
    val bl = new Array[CellBlockD](numBlocks)
    val ol = new Array[OutCellD](out)
    for (i <- 0 until in) {
      il(i) = inputLayer(i).combine(net2.getIn(i),dist,mutP,flipP,rnd)
    }
    for (i <- 0 until numBlocks) {
      bl(i) = cellBlocks(i).combine(net2.getMid(i),dist,mutP,flipP,rnd)
    }
    for (i <- 0 until out) {
      ol(i) = outputLayer(i).combine(net2.getOut(i),dist,mutP,flipP,rnd)
    }
    new RNND(il,bl,ol)
  }
  def feedData(inputData:Traversable[Array[Double]],actFun:Function1[Double,Double]) : Array[Array[Double]] = {
    val output = new Array[Array[Double]](inputData.size)
    var idx = 0
    for (in <- inputData) {
      output(idx) = activate(in,actFun)
      idx += 1
    }
    output
  }
  def evolinoFeed(inputData:Traversable[Array[Double]],actFun:Function1[Double,Double]) : DenseMatrix[Double] = {
    val stateSize = out + numBlocks*blockSize
    val dSize = inputData.size
    val mDat = new Array[Double](dSize*stateSize)
    var idx = 0
    for (db <- inputData) {
      val od = evolinoActivate(db,actFun)
      for (j <- 0 until stateSize) {
        mDat(idx*stateSize+j) = od(j)
      }
      idx += 1
    }
    new DenseMatrix(dSize,stateSize,mDat)
  }
  def linearRegression(inputData:Traversable[Array[Double]],targetMatrix:DenseMatrix[Double],actFun:Function1[Double,Double]) : DenseMatrix[Double] = {
    
    val X = evolinoFeed(inputData,actFun)
    val Xt = X.t
    val X2 = Xt * X
    val XXinv = LinearAlgebra.pinv(X2.toDense)
    val X3 = Xt * targetMatrix
    val X4 = XXinv * X3
    X4
  }
  def evolinoValidate(in2:Traversable[Array[Double]],out2:Traversable[Array[Double]],actFun:Function1[Double,Double],rMatrix:DenseMatrix[Double]) : Double = {
    var error = 0.0
    val output1 = evolinoFeed(in2,actFun)
    val pred = output1 * rMatrix
    var idx = 0
    for (row <- out2) {
      for (j <- 0 until row.length) {
        error += Math.pow(pred.apply(idx,j)-row(j),2)
      }
      idx += 1
    }
    error
  }
  def makeClone : RNND = {
    val il = new Array[InCellD](in)
    val bl = new Array[CellBlockD](numBlocks)
    val ol = new Array[OutCellD](out)
    for (i <- 0 until in) {
      il(i) = inputLayer(i).makeClone
    }
    for (i <- 0 until numBlocks) {
      bl(i) = cellBlocks(i).makeClone
    }
    for (i <- 0 until out) {
      ol(i) = outputLayer(i).makeClone
    }
    new RNND(il,bl,ol)
  }
  override def setFitness(f:Double) : Unit = {
    for (i <- 0 until in) {
      inputLayer(i).setFitness(f)
    }
    for (i <- 0 until numBlocks) {
      cellBlocks(i).setFitness(f)
    }
    for (i <- 0 until out) {
      outputLayer(i).setFitness(f)
    }
    fitness = f
  }
  def stimulate(actVal:Double,conn:NeuralConnsD) : Unit = {

    for ((dest,w) <- conn.getConns) {
      if (dest < in) {
        inputLayer(dest).stimulate(w*actVal)
      }
      else if (dest < midPoint) {
        val aux = dest - in
        val numG = aux % memGates
        val numB:Int = aux / memGates
        cellBlocks(numB).stimulate(w*actVal,numG)
	      
      }
      else {
        outputLayer(dest-midPoint).stimulate(w*actVal)
      }
    }
  }
  def reset : Unit = {
    firstInput = true
    for (b <- cellBlocks) {
      b.reset
    }
  }
  override def toString : String = {
    var srep = "<RNND>"
    for (i <- 0.until(in)) {
      srep += inputLayer(i)
    }
    for (i <- 0.until(numBlocks)) {
      srep += cellBlocks(i)
    }
    for (i <- 0.until(out)) {
      srep += outputLayer(i)
    }
    srep += "</RNND>"
    srep
	}
  def toXML : Elem = {
    val inL = new Array[Elem](in)
    for (i <- 0 until in) inL(i) = inputLayer(i).toXML
    val bL = new Array[Elem](numBlocks)
    for (i <- 0 until numBlocks) bL(i) = cellBlocks(i).toXML
    val oL = new Array[Elem](out)
    for (i <- 0 until out) oL(i) = outputLayer(i).toXML
    val tscope = TopScope
    val ip = new Elem(null,"InputLayer",null,tscope,inL: _*)
    val bp = new Elem(null,"BlockLayer",null,tscope,bL: _*)
    val op = new Elem(null,"OutputLayer",null,tscope,oL: _*)
    val res = <RNND>{ip}{bp}{op}</RNND>
    res
  }
  def fromXML(e:Elem) : Unit = {
    val ilElem = e \\ "InputLayer"
    val blElem = e \\ "BlockLayer"
    val olElem = e \\ "OutputLayer"
    val inputs = XMLOperator.customFilter(ilElem,"InCellD")
    val blocks = XMLOperator.customFilter(blElem,"CellBlockD")
    val outputs = XMLOperator.customFilter(olElem,"OutCellD")
    val in = new Array[InCellD](inputs.size)
    var idx = 0
    for (i <- inputs) {
      in(idx) = NeuralOps.fromXML(i)
      println(in(idx).toXML)
      idx += 1
    }
  }
  // SparseGraph[_ >: Nothing,_ >: Nothing]
  def toGraph :  SparseGraph[Int,Int] = {
    val graph = new SparseGraph[Int,Int]()
    val totalNodes = midPoint + out + numBlocks*3 + in + out
    var idx = 0
    for (i <- 0 until totalNodes) {
      graph.addVertex(i)
    }
    for (i <- 0 until in) {
      val cnn = inputLayer(i).getForward.conns
      for ((dest,(w,b)) <- cnn) {
        if (b) {
          graph.addEdge(idx,new Pair[Int](i,dest),EdgeType.DIRECTED)
          idx += 1
        }
      }
      val rnn = inputLayer(i).getRecurrent.conns
      for ((dest,(w,b)) <- rnn) {
        if (b) {
          graph.addEdge(idx,new Pair[Int](i,dest),EdgeType.DIRECTED)
          idx += 1
        }
      }
    }
    for (i <- 0 until numBlocks) {
      for (j <- 0 until (memGates-3)) {
        val cnn = cellBlocks(i).getForward(j).conns
        for ((dest,(w,b)) <- cnn) {
          if (b) {
            graph.addEdge(idx,new Pair[Int](in+i*memGates+j,dest),EdgeType.DIRECTED)
            idx += 1
          }
        }
        val cnn2 = cellBlocks(i).getRecurrent(j).conns
        for ((dest,(w,b)) <- cnn2) {
          if (b) {
            graph.addEdge(idx,new Pair[Int](in+i*memGates+j,dest),EdgeType.DIRECTED)
            idx += 1
          }
        }
      }
      
      val nC = cellBlocks(0).getNumOfCells
      for (j <- 0 until 3) {
        if (cellBlocks(i).gateBits(j)) {
          for (k <- 0 until cellBlocks(i).getNumOfCells) {
            val m = in+i*memGates+j+memGates-3
            graph.addEdge(idx,new Pair[Int](midPoint+out+i*nC+j,in+i*(nC+3)+k),EdgeType.DIRECTED)
            idx += 1
          }
        }
      }
      
    }
    for (i <- 0 until out) {
      val cnn = outputLayer(i).getRecurrent.conns
      for ((dest,(w,b)) <- cnn) {
        if (b) {
          graph.addEdge(idx,new Pair[Int](midPoint+i,dest),EdgeType.DIRECTED)
          idx += 1
        }
      }
    }
    //Lets add extra nodes connected to inputs to indicate input nodes
    for (i <- (totalNodes-in-out) until (totalNodes-out)) {
      graph.addEdge(idx,new Pair[Int](i-in,totalNodes-i-out),EdgeType.DIRECTED)
      idx += 1
    }
    for (i <- midPoint until (midPoint+out)) {
      graph.addEdge(idx,new Pair[Int](i,totalNodes-out+i-midPoint),EdgeType.DIRECTED)
      idx += 1
    }
    graph
  }
  /*Returns the state of this RNN which includes the activations of
   * output cells and memory cells
   */
  def getState : Array[Double] = {
    val r = new Array[Double](out+numBlocks*blockSize)
    for (i <- 0 until numBlocks) {
      for (j <- 0 until blockSize) {
        r(i*blockSize+j) = cellBlocks(i).memState(j)
      }
    }
    for (i <- midPoint until (midPoint+out)) {
      r(i) = outputLayer(i-midPoint).activation
    }
    r
  }
}