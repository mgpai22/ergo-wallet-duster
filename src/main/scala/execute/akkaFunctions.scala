package execute

import configs.{conf, serviceOwnerConf}
import contracts.PhoenixContracts
import execute.HodlCalulations.{extractPrecisionFactor, hodlMintAmountFromERG, hodlPrice}
import org.ergoplatform.appkit.{Address, ErgoContract, InputBox}
import org.ergoplatform.sdk.ErgoToken
import special.collection.Coll
import utils.{BoxAPI, BoxJson, ContractCompile, OutBoxes, TransactionHelper, explorerApi}

import java.util
import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions.`collection asJava`
import scala.collection.mutable.ListBuffer
import scala.collection.mutable

class ErgoScriptConstantDecodeError(message: String) extends Exception(message)

class akkaFunctions {

  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext
  private val serviceFilePath = "serviceOwner.json"
  private val contractConfFilePath = "contracts.json"
  private lazy val serviceConf = serviceOwnerConf.read(serviceFilePath)
  private lazy val contractsConf = conf.read(contractConfFilePath)

  private val walletMnemonic = serviceConf.txOperatorMnemonic
  private val walletMnemonicPw = serviceConf.txOperatorMnemonicPw
  private val txHelper =
    new TransactionHelper(this.ctx, walletMnemonic, walletMnemonicPw)
  private val boxAPIObj = new BoxAPI(serviceConf.apiUrl, serviceConf.nodeUrl)
  private val explorer = new explorerApi()
  private val compiler = new ContractCompile(ctx)
  private val outBoxObj = new OutBoxes(ctx)

  private val feeScript: String =
    PhoenixContracts.phoenix_v1_hodlcoin_fee.contractScript

  private val phoenixScript: String =
    PhoenixContracts.phoenix_v1_hodlcoin_bank.contractScript

  private val feeContract: ErgoContract = compiler.compileFeeContract(
    feeScript,
    serviceConf.minMinerFee
  )

  private val phoenixContract: ErgoContract =
    compiler.compileBankContract(phoenixScript, feeContract)

  private val proxyAddress = compiler
    .compileProxyContract(
      PhoenixContracts.phoenix_v1_hodlcoin_proxy.contractScript,
      serviceConf.minTxOperatorFee
    )
    .toAddress

  println("Service Runner Address: " + txHelper.senderAddress)
  def burnAmount(hodlBoxIn: InputBox, hodlBurnAmt: Long): (Long, Long, Long) = {
    val feeDenom = 1000L

    val devFee =
      hodlBoxIn.getRegisters.get(3).getValue.asInstanceOf[Long] // R7
    val bankFee =
      hodlBoxIn.getRegisters.get(4).getValue.asInstanceOf[Long] // R8

    val price = hodlPrice(hodlBoxIn)
    val precisionFactor = extractPrecisionFactor(hodlBoxIn)
    val beforeFees = hodlBurnAmt * price / precisionFactor
    val bankFeeAmount: Long = (beforeFees * bankFee) / feeDenom
    val devFeeAmount: Long = (beforeFees * devFee) / feeDenom
    val expectedAmountWithdrawn: Long =
      beforeFees - bankFeeAmount - devFeeAmount
    (expectedAmountWithdrawn, devFeeAmount, bankFeeAmount)
  }

  def mintHodl(proxyInputs: Seq[InputBox], bankInput: InputBox): InputBox = {
    var currentBankInput = bankInput

    proxyInputs.foreach(proxyInput => {

      try {
        val recipientAddress = {
          try {
            new org.ergoplatform.appkit.SigmaProp(
              proxyInput.getRegisters
                .get(0)
                .getValue
                .asInstanceOf[special.sigma.SigmaProp]
            ).toAddress(ctx.getNetworkType)
          } catch {
            case e: Exception =>
              throw new ErgoScriptConstantDecodeError(
                "Error decoding recipient address"
              )
          }
        }

        val hodlSingleton = new ErgoToken(
          proxyInput.getRegisters
            .get(1)
            .getValue
            .asInstanceOf[Coll[Byte]]
            .toArray,
          1L
        )

        val hodlTokenId: Array[Byte] =
          proxyInput.getRegisters
            .get(2)
            .getValue
            .asInstanceOf[Coll[Byte]]
            .toArray

        val totalTokenSupply =
          currentBankInput.getRegisters.get(0).getValue.asInstanceOf[Long]
        val precisionFactor =
          currentBankInput.getRegisters.get(1).getValue.asInstanceOf[Long]
        val minBankValue =
          currentBankInput.getRegisters.get(2).getValue.asInstanceOf[Long]
        val devFee =
          currentBankInput.getRegisters.get(3).getValue.asInstanceOf[Long]
        val bankFee =
          currentBankInput.getRegisters.get(4).getValue.asInstanceOf[Long]

        val minBoxValue =
          proxyInput.getRegisters.get(3).getValue.asInstanceOf[Long]
        val minerFee =
          proxyInput.getRegisters.get(4).getValue.asInstanceOf[Long]

        val txOperatorFee = proxyInput.getRegisters.get(5).getValue.asInstanceOf[Long]

        val ergMintAmount =
          proxyInput.getValue - minBoxValue - minerFee - txOperatorFee

        val hodlMintAmount =
          hodlMintAmountFromERG(currentBankInput, ergMintAmount)

        val hodlOutBoxHodlTokenAmount =
          currentBankInput.getTokens.get(1).getValue - hodlMintAmount

        val phoenixContract = Address
          .fromPropositionBytes(
            ctx.getNetworkType,
            currentBankInput.toErgoValue.getValue.propositionBytes.toArray
          )
          .toErgoContract

        val hodlOutBox = outBoxObj.hodlBankBox(
          phoenixContract,
          hodlSingleton,
          new ErgoToken(hodlTokenId, hodlOutBoxHodlTokenAmount.toLong),
          totalTokenSupply,
          precisionFactor,
          minBankValue,
          bankFee,
          devFee,
          currentBankInput.getValue + ergMintAmount
        )

        val recipientBox = outBoxObj.hodlMintBox(
          recipientAddress,
          new ErgoToken(hodlTokenId, hodlMintAmount.toLong)
        )

        val unsignedTransaction = txHelper.buildUnsignedTransaction(
          inputs = Array(currentBankInput, proxyInput),
          outputs = Array(hodlOutBox, recipientBox),
          fee = minerFee
        )
        val signedTx = txHelper.signTransaction(unsignedTransaction)
        val txHash =
          txHelper.sendTx(signedTx)

        println("Mint Transaction Submitted: " + txHash)

        Thread.sleep(500)

        currentBankInput = signedTx.getOutputsToSpend.get(0)
      } catch {
        case e: Exception =>
          println(
            "error minting with proxy input: " + proxyInput.getId
              .toString() + " " + e
          )
      }
    })
    currentBankInput
  }

  def burnHodl(proxyInputs: Seq[InputBox], bankInput: InputBox): InputBox = {

    var currentBankInput = bankInput

    proxyInputs.foreach(proxyInput => {
      try {
        val recipientAddress = {
          try {
            new org.ergoplatform.appkit.SigmaProp(
              proxyInput.getRegisters
                .get(0)
                .getValue
                .asInstanceOf[special.sigma.SigmaProp]
            ).toAddress(ctx.getNetworkType)
          } catch {
            case _: Exception =>
              throw new ErgoScriptConstantDecodeError(
                "Error decoding artist address"
              )
          }
        }

        val hodlSingleton = new ErgoToken(
          proxyInput.getRegisters
            .get(1)
            .getValue
            .asInstanceOf[Coll[Byte]]
            .toArray,
          1L
        )

        val hodlTokenId: Array[Byte] =
          proxyInput.getRegisters
            .get(2)
            .getValue
            .asInstanceOf[Coll[Byte]]
            .toArray

        val totalTokenSupply =
          currentBankInput.getRegisters.get(0).getValue.asInstanceOf[Long]
        val precisionFactor =
          currentBankInput.getRegisters.get(1).getValue.asInstanceOf[Long]
        val minBankValue =
          currentBankInput.getRegisters.get(2).getValue.asInstanceOf[Long]
        val devFee =
          currentBankInput.getRegisters.get(3).getValue.asInstanceOf[Long]
        val bankFee =
          currentBankInput.getRegisters.get(4).getValue.asInstanceOf[Long]
        val minerFee =
          proxyInput.getRegisters.get(4).getValue.asInstanceOf[Long]

        val hodlDummyToken = new ErgoToken(
          proxyInput.getRegisters
            .get(2)
            .getValue
            .asInstanceOf[Coll[Byte]]
            .toArray,
          1L
        )
        val hodlBurnAmount = proxyInput.getTokens.asScala
          .filter(t => t.getId.toString() == hodlDummyToken.getId.toString())
          .map(_.getValue)
          .sum

        val (userBoxAmount, devFeeAmount, bankFeeAmount) =
          burnAmount(currentBankInput, hodlBurnAmount)

        val bankBoxOutAmount =
          currentBankInput.getValue - userBoxAmount - devFeeAmount

        val hodlOutBoxHodlTokenAmount =
          currentBankInput.getTokens.get(1).getValue + hodlBurnAmount

        val phoenixContract = Address
          .fromPropositionBytes(
            ctx.getNetworkType,
            currentBankInput.toErgoValue.getValue.propositionBytes.toArray
          )
          .toErgoContract

        val hodlOutBox = outBoxObj.hodlBankBox(
          phoenixContract,
          hodlSingleton,
          new ErgoToken(hodlTokenId, hodlOutBoxHodlTokenAmount),
          totalTokenSupply,
          precisionFactor,
          minBankValue,
          bankFee,
          devFee,
          bankBoxOutAmount
        )

        val recipientBox =
          outBoxObj.simpleOutBox(recipientAddress, userBoxAmount)

        //TODO: extract feeContract from constants

        val devFeeBox =
          outBoxObj.simpleOutBox(feeContract.toAddress, devFeeAmount)

        val unsignedTransaction = txHelper.buildUnsignedTransaction(
          inputs = Array(currentBankInput, proxyInput),
          outputs = Array(hodlOutBox, recipientBox, devFeeBox),
          fee = minerFee
        )

        val signedTx = txHelper.signTransaction(unsignedTransaction)

        val txHash =
          txHelper.sendTx(signedTx)

        println("Burn Transaction Submitted: " + txHash)

        currentBankInput = signedTx.getOutputsToSpend.get(0)
      } catch {
        case e: Exception =>
          println(
            "error burning with proxy input: " + proxyInput.getId
              .toString() + " " + e
          )
      }

    })
    currentBankInput
  }

  def sortProxyInputs(
      inputs: Seq[InputBox],
      singleton: ErgoToken
  ): (Seq[InputBox], Seq[InputBox]) = {
    val (burnInputs, mintInputs) =
      inputs.partition(box => {
        // if hodl tokens exists it goes to the first variable
        val dummyHodlToken = new ErgoToken(
          box.getRegisters.get(2).getValue.asInstanceOf[Coll[Byte]].toArray,
          1L
        )
        box.getTokens.asScala
          .exists(t => t.getId.toString() == dummyHodlToken.getId.toString())
      })

    def sortBySingleton(box: InputBox): Boolean = {
      new ErgoToken(
        box.getRegisters.get(1).getValue.asInstanceOf[Coll[Byte]].toArray,
        1L
      ).getId.toString() == singleton.getId.toString()
    }

    (mintInputs.filter(sortBySingleton), burnInputs.filter(sortBySingleton))
  }

  def mint(boxes: Array[BoxJson]): Unit = {

    val validatedBoxInputs: Array[InputBox] = boxes
      .filter(box =>
        validateBox(
          box,
          serviceConf.minBoxValue,
          serviceConf.minMinerFee,
          serviceConf.minTxOperatorFee
        )
      )
      .map(boxAPIObj.convertJsonBoxToInputBox)

    if (validatedBoxInputs.length == 0) {
      println("No Valid Boxes Found")
      return
    }

    val bankSingleton = new ErgoToken(
      validatedBoxInputs.head.getRegisters
        .get(1)
        .getValue
        .asInstanceOf[Coll[Byte]]
        .toArray,
      1L
    )

    val bankBoxFromApi: InputBox = {
      try {
        val boxID = explorer
          .getUnspentBoxFromTokenID(bankSingleton.getId.toString())
          .getBoxId
        explorer.getUnspentBoxFromMempool(boxID)
      } catch {
        case e: Exception => println("error getting state box: " + e); return
      }
    }

    val (mintInputs, burnInputs) =
      sortProxyInputs(validatedBoxInputs, bankSingleton)

    val bankBoxAfterMints = {
      try {
        mintHodl(mintInputs, bankBoxFromApi)
      } catch {
        case e: Exception =>
          println("error minting: " + e)
          bankBoxFromApi
      }
    }
    try {
      burnHodl(burnInputs, bankBoxAfterMints)
    } catch {
      case e: Exception => println("error burning: " + e)
    }
  }

  def main(): Unit = {

    val boxes =
      boxAPIObj
        .getUnspentBoxesFromApi(proxyAddress.toString, selectAll = true)
        .items

    mintWithRetry(boxes)

  }

  def mintWithRetry(boxes: Array[BoxJson]): Unit = {
    try {
      mint(boxes) // Call the mint function
    } catch {
      case _: Throwable => // Catch any error thrown
        if (boxes.nonEmpty) {
          mintWithRetry(boxes.tail) // Call mintWithRetry with the first element deleted from the boxes array
        }
    }
  }

  def validateBox(
      box: BoxJson,
      minBoxValue: Long,
      minerFee: Long,
      minTxOperatorFee: Long
  ): Boolean = {
      box.boxId != "831eb559eac7b4880358502a0e83b4dd775b29f95d30040a65c4fb99d11082b2" &&
    box.additionalRegisters.R4 != null &&
    box.additionalRegisters.R5.serializedValue != null &&
    box.additionalRegisters.R6.serializedValue != null &&
    box.additionalRegisters.R7.renderedValue.toLong >= minBoxValue &&
    box.additionalRegisters.R8.renderedValue.toLong >= minerFee &&
      box.additionalRegisters.R9.renderedValue.toLong >= minTxOperatorFee
  }

}
