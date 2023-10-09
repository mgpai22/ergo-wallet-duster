package app

import configs.serviceOwnerConf
import contracts.PhoenixContracts
import execute.{ChainedTxnExecutor, Client}
import org.ergoplatform.appkit.{Address, ErgoContract, InputBox, OutBox}
import org.ergoplatform.sdk.ErgoToken
import utils.{BoxAPI, ContractCompile, OutBoxes, TransactionHelper, explorerApi}

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object Duster extends App {

  val arglist = args.toList
  type OptionMap = Map[Symbol, String]

  @tailrec
  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    list match {
      case Nil => map
      case "--conf" :: value :: tail =>
        nextOption(map ++ Map('conf -> value.toString), tail)
      case "--dryrun" :: tail =>
        nextOption(map ++ Map('dryrun -> "true"), tail)
      case option :: tail =>
        println("Unknown option " + option)
        sys.exit(1)
    }
  }

  private val options = nextOption(Map(), arglist)

  private val confFilePath = options.get('conf)
  private val dryRun = options.contains('dryrun)

  confFilePath match {
    case Some(path) => // Continue processing
    case None =>
      println("Configuration file path not provided.")
      sys.exit(1)
  }

  private val serviceOwnerConfigFilePath: String = confFilePath.get
  private val serviceOwnerConfig =
    serviceOwnerConf.read(serviceOwnerConfigFilePath)

  if (serviceOwnerConfig.txFeeNanoERG < 1000000) {
    println("error: fee must be greater than or equal to 1000000")
    sys.exit(1)
  }

  val recipient =
    try {
      Address.create(serviceOwnerConfig.recipient)
    } catch {
      case e: Exception =>
        println("error: recipient must be a proper ergo address"); sys.exit(1)
    }

  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext

  val boxAPIObj =
    new BoxAPI(serviceOwnerConfig.apiUrl, serviceOwnerConfig.nodeUrl)
  val outBoxObj = new OutBoxes(ctx)
  val txHelper = new TransactionHelper(
    ctx,
    serviceOwnerConfig.txOperatorMnemonic,
    proverIndex = serviceOwnerConfig.addressIndex
  )

  println("Connected Address: " + txHelper.senderAddress.toString)

  Thread.sleep(3000)

  val inputboxes =
    try {
      boxAPIObj
        .getUnspentBoxesFromApi(
          txHelper.senderAddress.toString
        )
        .items
        .map(boxAPIObj.convertJsonBoxToInputBox)
    } catch {
      case e: Exception =>
        println(
          "error: issue getting inputs (ERGs), please make sure your wallet has enough ERGs"
        ); sys.exit(1)
    }

  val outBoxList = new ListBuffer[OutBox]

  val out = outBoxObj.simpleOutBox(
    recipient,
    serviceOwnerConfig.amountNanoERGPerBox
  )

  for (x <- 0 until (serviceOwnerConfig.amountBoxes)) {

    outBoxList.append(out)
  }

  val unSignedTx =
    txHelper.buildUnsignedTransaction(
      inputboxes,
      outBoxList,
      fee = serviceOwnerConfig.txFeeNanoERG
    )

  val signedTx = txHelper.signTransaction(
    unSignedTx,
    proverIndex = serviceOwnerConfig.addressIndex
  )

  val txId =
    if (dryRun) signedTx.getId.toString
    else
      try {
        txHelper.sendTx(signedTx).replace("\"", "")
      } catch {
        case e: Exception =>
          println("error: issue submitting transaction"); sys.exit(1)
      }

  println(
    s"sent transaction of ${unSignedTx.getOutputs.size()} outputs to ${serviceOwnerConfig.recipient} using a fee of ${serviceOwnerConfig.txFeeNanoERG * math
      .pow(10, -9)} ERGs $txId"
  )

  sys.exit(0)
}
