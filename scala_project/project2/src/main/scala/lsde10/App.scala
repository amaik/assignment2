package lsde10

import dk.dma.ais.sentence.Vdm
import dk.dma.ais.message.AisMessage
import dk.dma.ais.message.AisPositionMessage
import dk.dma.ais.message.AisMessageException
import dk.dma.ais.binary.SixbitException
import dk.dma.ais.sentence.SentenceException
import org.apache.spark.mllib.rdd.RDDFunctions._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.compress.GzipCodec


/**
 * @author ${user.name}
 */
object App {
	
	//spark-shell --jars ais-lib-messages-2.0.jar,enav-model-0.3.jar --master yarn
	//spark-submit --class lsde10.App --master yarn --deploy-mode cluster <jar>

  def main(args : Array[String]) {
  
	def fun (line: String) : AisMessage = {
		try{
			val vdm = new Vdm()
			vdm.parse(line)
			val msg = AisMessage.getInstance(vdm)
			if(msg.isInstanceOf[AisPositionMessage])
				return msg
			else 
				return null
		}
		catch{
			case ex: AisMessageException =>{}
			case ex: SixbitException =>{}
			case ex: SentenceException =>{}
		}
		return null
	}
  
  
    val conf = new SparkConf().setAppName("Finding Outage")
	val sc = new SparkContext(conf)
	var path = "/user/hannesm/lsde/ais/10/01/00-00.txt.gz"
	
	for (x <- 0 to 3){
		for (y <- 0 to 59){
			if(x!=0 || y != 0){
			path += ",/user/hannesm/lsde/ais/10/01/0" + x.toString() + "-"
			if( y >= 10)
				path += y.toString()
			else
				path += "0" + y.toString()
			path += ".txt.gz"
			}
		}
	}
	
	
	
	
	var text = sc.wholeTextFiles(path).flatMapValues(y => y.split("\n"))
	
	//val text = sc.wholeTextFiles("/user/hannesm/lsde/ais/10/01/*.txt.gz").values.flatMap(file => {
	//val lines = file.split("\n")
	//val id = lines.head.split(" ").head
	//lines.tail.map((id, _))
	//})
	//val mmsi = clean.map(p => (p._2.getUserId(),p._1.substring(0,p._1.indexOf(".")).toInt))

	var data = text.map(p => {
			val index = p._1.indexOf("/ais/10/")
			(p._1.substring(index + 8,index + 16).replace("/","").replace("-",""),p._2)
		})//get timestamp from textfile
		.filter(p => p._2.contains("!"))
		.map(p => (p._1, fun(p._2.substring(p._2.indexOf("!"))))) //map to (timestamp,aismessage)
		.filter(p => if(p._2 == null) false else true)
		.map(p => (p._2.getUserId(),(p._1.toInt, p._2.asInstanceOf[AisPositionMessage])))//map to (mmsi,(timestamp,aismessage))
		.sortBy(p => (p._1,p._2._1))//sortBy (mmsi, timestamp)
		.sliding(2).collect({
			case Array((key1, val1), (key2, val2)) if key1 == key2 => (key1, val2._1 - val1._1)
		})
	data.cache()
	
	//clear.distinct()
	
	var reduced = data.reduceByKey(Math.max(_, _))	
	reduced.take(10).foreach(println)
	
	reduced.saveAsTextFile("/user/lsde10/threehours",classOf[GzipCodec])
  }

}