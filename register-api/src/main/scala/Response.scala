import scala.beans.BeanProperty


case class Response(@BeanProperty var message: String){
  /**
   * Empty constructor needed by jackson (for AWS Lambda)
   * @return
   */
  def this() = this(message="")

}