package initialize

import configs.{conf, serviceOwnerConf}
import contracts.PhoenixContracts
import execute.Client
import org.ergoplatform.appkit.{ErgoContract, Parameters}
import org.ergoplatform.sdk.ErgoToken
import utils.{ContractCompile, InputBoxes, OutBoxes, TransactionHelper}

object initialize extends App {
  private val client: Client = new Client()
  client.setClient
  val ctx = client.getContext
  val serviceFilePath = "serviceOwner.json"
  val contractConfFilePath = "contracts.json"
  val serviceConf = serviceOwnerConf.read(serviceFilePath)

  val walletMnemonic = serviceConf.txOperatorMnemonic
  val walletMnemonicPw = serviceConf.txOperatorMnemonicPw
  val txHelper =
    new TransactionHelper(this.ctx, walletMnemonic, walletMnemonicPw)

  val inputBoxesObj = new InputBoxes(ctx)
  val outBoxObj = new OutBoxes(ctx)
  val compiler = new ContractCompile(ctx)

  private val feeScript: String =
    PhoenixContracts.phoenix_v1_hodlcoin_fee.contractScript

  private val phoenixScript: String = // make sure to change depending on testnet or mainnet
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

  val hodlDecimals = 9
  val totalTokenSupply = (97739924 * math.pow(10, hodlDecimals)).toLong
  val precisionFactor = 1000000L
  val minBankValue = 1000000L
  val bankFeeNum = 30L
  val devFeeNum = 3L

  val hodlDecimal = 9

  val genesisInput =
    inputBoxesObj.getInputs(
      Array(2 * Parameters.OneErg),
      txHelper.senderAddress
    )

  val singleton = outBoxObj.tokenHelper(
    genesisInput.head,
    "Phoenix hodlERG3 Bank Singleton",
    "Phoenix hodlERG3 bank identification token",
    1L,
    0
  )

  val singletonMintOutput =
    outBoxObj.tokenMintOutBox(singleton, txHelper.senderAddress)

  val unsignedSingletonMintTx = txHelper.buildUnsignedTransaction(
    inputs = genesisInput,
    outputs = Array(singletonMintOutput)
  )

  val singletonMintSignedTransaction =
    txHelper.signTransaction(unsignedSingletonMintTx)

  val singletonMintTxHash = txHelper.sendTx(singletonMintSignedTransaction)

  println("Singleton Mint Transaction: " + singletonMintTxHash)

  val hodlTokenMintInput =
    singletonMintSignedTransaction.getOutputsToSpend.get(2)

  val hodlTokens = outBoxObj.tokenHelper(
    hodlTokenMintInput,
    "hodlERG3",
    "The Phoenix Finance implementation of the hodlCoin protocol: hodlERG 3%",
    totalTokenSupply,
    hodlDecimal
  )

  val hodlTokenMintOutput =
    outBoxObj.tokenMintOutBox(hodlTokens, txHelper.senderAddress)

  val unsignedHodlMintTx = txHelper.buildUnsignedTransaction(
    inputs = Array(hodlTokenMintInput),
    outputs = Array(hodlTokenMintOutput)
  )

  val hodlMintSignedTx = txHelper.signTransaction(unsignedHodlMintTx)

  val hodlMintTxHash = txHelper.sendTx(hodlMintSignedTx)

  println("Hodl Mint Transaction: " + hodlMintTxHash)

  val hodlBoxInput = Array(
    singletonMintSignedTransaction.getOutputsToSpend.get(0),
    hodlMintSignedTx.getOutputsToSpend.get(0),
    hodlMintSignedTx.getOutputsToSpend.get(2)
  )

  val hodlBoxOutput = outBoxObj.hodlBankBox(
    phoenixContract,
    singleton,
    new ErgoToken(
      hodlMintSignedTx.getOutputsToSpend.get(0).getTokens.get(0).getId.toString,
      totalTokenSupply - (1L * math.pow(10, hodlDecimals)).toLong
    ),
    totalTokenSupply,
    precisionFactor,
    minBankValue,
    bankFeeNum,
    devFeeNum,
    Parameters.OneErg
  )

  val unsignedHodlBoxTx = txHelper.buildUnsignedTransaction(
    inputs = hodlBoxInput,
    outputs = Array(hodlBoxOutput),
    tokensToBurn = Array(
      new ErgoToken(
        hodlMintSignedTx.getOutputsToSpend
          .get(0)
          .getTokens
          .get(0)
          .getId
          .toString,
        (1L * math.pow(10, hodlDecimals)).toLong
      )
    )
  )

  val signedHodlBoxTx = txHelper.signTransaction(unsignedHodlBoxTx)

  val hodlBoxTxHash = txHelper.sendTx(signedHodlBoxTx)

  println("Hodl Box Transaction: " + hodlBoxTxHash)

  new conf(
    phoenixContract.toAddress.toString,
    singletonMintSignedTransaction.getOutputsToSpend
      .get(0)
      .getTokens
      .get(0)
      .getId
      .toString,
    hodlMintSignedTx.getOutputsToSpend.get(0).getTokens.get(0).getId.toString,
    feeContract.toAddress.toString,
    proxyAddress.toString
  ).write(contractConfFilePath)
}
