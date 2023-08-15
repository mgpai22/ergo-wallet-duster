package app

import configs.serviceOwnerConf
import contracts.PhoenixContracts
import execute.{ChainedTxnExecutor, Client}
import org.ergoplatform.appkit.{ErgoContract, InputBox}
import org.ergoplatform.sdk.ErgoToken
import utils.{BoxAPI, ContractCompile, TransactionHelper, explorerApi}

import scala.annotation.tailrec

object GreaseFire extends App {

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

  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext

  val explorerClient = new explorerApi()

  val compiler = new ContractCompile(ctx)

  val feeScript: String =
    PhoenixContracts.phoenix_v1_hodlcoin_fee.contractScript

// uncomment this for testnet
//  val feeContract: ErgoContract = Address
//    .create(
//      "3m1wJ88xGVWx79p9oenKvCTwXp4o4J6n7KnbF9N3uRcmvtH1MAegmuBxsmB7kfjYTie2Rpp6He9kh3XJBNwwqr5HUiPsD1jLQcJ15kYneii6JN8UfSPcHb6mZA8k3PB7pcQUFox416Tv6AmrReVtVHaP1VGxfvM7WRNEbJDbu8wU7fwkRyqxPBbJXTcUMogpg9XVstiQHPmndzqbdREUSNVY5iuV68GYaYc7YuBqhax8oVLxWPCWGoaZEevPtsEnxXTKvR94H6KfPW1xwRFztZMpwzyv9PNVSqmXXCqhe8ryb3RkSEsk59CBcEMoNxkEA2zi3BMWV65U3h5PHa1d1X1ePS7fBaPTHRa5roSs6AhEdUK1tPjun7N1Jqo88SbGeWrY4hbNjCVGjAcx9ZC9vfE4sZdUtyCQyguzEwYdfdb6Xgn9YaKwC3LeR9Q2jDPt3wEtLrPdpAhCpn8b9MzaH79fdM7pnxxMj7NqdrFJUZ8Aqq3eb4CrBj4y8eUztPuuBMA7kdJ4GHLy8t4aPSUhkSPVdFcTGTbvv4e83UMZdEAL1GeqA3CJjTYRiiK3kKsphdXuxwmZHPPBAqSnm3H2nnAbb"
//    )
//    .toErgoContract

  val feeContract: ErgoContract = compiler.compileFeeContract(
    feeScript,
    1000000L
  )

  val bankSingleton =
    new ErgoToken(serviceOwnerConfig.bankBoxSingletonTokenId, 1L)

  private val walletMnemonic = serviceOwnerConfig.txOperatorMnemonic
  private val walletMnemonicPw = serviceOwnerConfig.txOperatorMnemonicPw

  private val boxAPIObj =
    new BoxAPI(serviceOwnerConfig.apiUrl, serviceOwnerConfig.nodeUrl)

  private val txHelper =
    new TransactionHelper(
      this.ctx,
      walletMnemonic,
      walletMnemonicPw,
      serviceOwnerConfig.addressIndex
    )

  val boxes =
    boxAPIObj
      .getUnspentBoxesFromApi(txHelper.senderAddress.toString, selectAll = true)
      .items
      .map(boxAPIObj.convertJsonBoxToInputBox)

  val ergAmount = boxes.map(_.getValue).sum

  if (ergAmount < 2000000L) {
    println("Not enough ERGs for txn")
    sys.exit(1)
  }

  val bankBox: InputBox = {
    try {
      val boxID = explorerClient
        .getUnspentBoxFromTokenID(bankSingleton.getId.toString())
        .getBoxId
      explorerClient.getUnspentBoxFromMempool(boxID)
    } catch {
      case e: Exception => println("error getting bank box: " + e); null
    }
  }

  if (bankBox == null) {
    println("No bank box found")
    sys.exit(1)
  }

  try {
    ChainedTxnExecutor.main(
      bankBox,
      boxes,
      feeContract,
      ctx,
      txHelper,
      dryRun
    )
  } catch {
    case e: Exception => println("error executing txns: " + e); sys.exit(1)
  }

}
