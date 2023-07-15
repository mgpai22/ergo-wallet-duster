package utils

import org.ergoplatform.appkit._
import org.ergoplatform.sdk.ErgoToken

import java.util
import scala.collection.JavaConverters._

class InputBoxes(val ctx: BlockchainContext) {

  def getBoxById(id: String): Array[InputBox] = {
    this.ctx.getBoxesById(id)
  }

  def getInputs(
      amountList: Seq[Long],
      senderAddress: Address,
      tokenListParam: Option[Seq[Seq[String]]] = None,
      amountTokens: Option[Seq[Seq[Long]]] = None
  ): Seq[InputBox] = {
    val amountTotal: Long = amountList.sum
    val tokens = (tokenListParam, amountTokens) match {
      case (Some(tlp), Some(at)) =>
        tlp
          .zip(at)
          .flatMap { case (token, amounts) =>
            token.zip(amounts).map { case (t, a) => new ErgoToken(t, a) }
          }
          .asJava
      case (Some(tlp), None) =>
        tlp.flatten.map(new ErgoToken(_, 1)).asJava
      case _ =>
        new util.ArrayList[ErgoToken]()
    }

    BoxOperations
      .createForSender(senderAddress, this.ctx)
      .withAmountToSpend(amountTotal)
      .withTokensToSpend(tokens)
      .withInputBoxesLoader(new ExplorerAndPoolUnspentBoxesLoader())
      .loadTop()
      .asScala
  }

}
