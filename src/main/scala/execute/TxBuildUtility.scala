package execute

import org.ergoplatform.appkit.BlockchainContext
import utils.{OutBoxes, TransactionHelper, explorerApi}

import scala.collection.JavaConverters._

class TxBuildUtility(
    val ctx: BlockchainContext,
    txOperatorMnemonic: String,
    txOperatorMnemonicPw: String
) {

  private val outBoxObj = new OutBoxes(ctx)
  private val txHelper = new TransactionHelper(
    ctx = ctx,
    walletMnemonic = txOperatorMnemonic,
    mnemonicPassword = txOperatorMnemonicPw
  )

}
