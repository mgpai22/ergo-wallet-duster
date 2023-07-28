package utils

import org.ergoplatform.appkit.impl.{Eip4TokenBuilder, ErgoTreeContract}
import org.ergoplatform.appkit._
import org.ergoplatform.sdk.ErgoToken
import work.lithos.plasma.collections.LocalPlasmaMap

import java.nio.charset.StandardCharsets
import java.util
import scala.collection.mutable.ListBuffer

class OutBoxes(ctx: BlockchainContext) {

  private val minAmount = 1000000L
  private val txBuilder = this.ctx.newTxBuilder()

  def tokenHelper(
      inputBox: InputBox,
      name: String,
      description: String,
      tokenAmount: Long,
      tokenDecimals: Int
  ): Eip4Token = {
    new Eip4Token(
      inputBox.getId.toString,
      tokenAmount,
      name,
      description,
      tokenDecimals
    )
  }

  def collectionTokenHelper(
      inputBox: InputBox,
      name: String,
      description: String,
      tokenAmount: Long,
      tokenDecimals: Int
  ): Eip4Token = {

    Eip4TokenBuilder.buildNftPictureToken(
      inputBox.getId.toString,
      tokenAmount,
      name,
      description,
      tokenDecimals,
      Array(0.toByte),
      "link"
    )
  }

  def tokenMintOutBox(
      token: Eip4Token,
      receiver: Address,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .mintToken(token)
      .contract(
        new ErgoTreeContract(
          receiver.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

  def tokenOutBox(
      token: Seq[ErgoToken],
      receiver: Address,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .tokens(token: _*)
      .contract(
        new ErgoTreeContract(
          receiver.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

  def optionalTokenOutBox(
      token: Seq[ErgoToken],
      receiver: Address,
      amount: Long = minAmount
  ): OutBox = {
    val box = this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .contract(
        new ErgoTreeContract(
          receiver.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )

    if (token.nonEmpty) {
      box.tokens(token: _*)
    }
    box.build()
  }

  def genericContractBox(
      contract: ErgoContract,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .contract(contract)
      .build()
  }

  def simpleOutBox(
      senderAddress: Address,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .contract(
        new ErgoTreeContract(
          senderAddress.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

  def hodlBankBox(
      hodlContract: ErgoContract,
      hodlSingleton: ErgoToken,
      hodlToken: ErgoToken,
      totalTokenSupply: Long,
      precisionFactor: Long,
      minBankValue: Long,
      bankFeeNum: Long,
      devFeeNum: Long,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .tokens(hodlSingleton, hodlToken)
      .registers(
        ErgoValue.of(totalTokenSupply),
        ErgoValue.of(precisionFactor),
        ErgoValue.of(minBankValue),
        ErgoValue.of(devFeeNum),
        ErgoValue.of(bankFeeNum)
      )
      .contract(hodlContract)
      .build()
  }

  def hodlMintBox(
      recipientAddress: Address,
      hodlTokens: ErgoToken,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .tokens(hodlTokens)
      .contract(
        new ErgoTreeContract(
          recipientAddress.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

  def hodlBurnBox(
      recipientAddress: Address,
      hodlTokens: ErgoToken,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .tokens(hodlTokens)
      .contract(
        new ErgoTreeContract(
          recipientAddress.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

  def proxyMintInputBox(
      proxyContract: ErgoContract,
      buyerPk: Address,
      bankSingleton: ErgoToken,
      hodlToken: ErgoToken,
      minBoxValue: Long,
      minerFee: Long,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .registers(
        ErgoValue.of(buyerPk.getPublicKey),
        ErgoValue.of(bankSingleton.getId.getBytes),
        ErgoValue.of(hodlToken.getId.getBytes),
        ErgoValue.of(minBoxValue),
        ErgoValue.of(minerFee)
      )
      .contract(proxyContract)
      .build()
  }

  def proxyBurnInputBox(
      proxyContract: ErgoContract,
      buyerPk: Address,
      bankSingleton: ErgoToken,
      hodlToken: ErgoToken,
      minBoxValue: Long,
      minerFee: Long,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .tokens(hodlToken)
      .registers(
        ErgoValue.of(buyerPk.getPublicKey),
        ErgoValue.of(bankSingleton.getId.getBytes),
        ErgoValue.of(hodlToken.getId.getBytes),
        ErgoValue.of(minBoxValue),
        ErgoValue.of(minerFee)
      )
      .contract(proxyContract)
      .build()
  }

}
