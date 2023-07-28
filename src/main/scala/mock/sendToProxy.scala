package mock

import configs.{ServiceOwnerConfig, conf, serviceOwnerConf}
import contracts.PhoenixContracts
import execute.Client
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  InputBox,
  Parameters,
  SignedTransaction
}
import org.ergoplatform.sdk.ErgoToken
import utils.{ContractCompile, InputBoxes, OutBoxes, TransactionHelper}

object sendToProxyMint extends App {
  private val client: Client = new Client()
  client.setClient
  val ctx: BlockchainContext = client.getContext
  val serviceFilePath = "serviceOwner.json"
  val contractConfFilePath = "contracts.json"
  val serviceConf: ServiceOwnerConfig = serviceOwnerConf.read(serviceFilePath)
  private val contractConf = conf.read(contractConfFilePath)

  val walletMnemonic: String = serviceConf.txOperatorMnemonic
  val walletMnemonicPw: String = serviceConf.txOperatorMnemonicPw
  val txHelper =
    new TransactionHelper(this.ctx, walletMnemonic, walletMnemonicPw)

  val inputBoxesObj = new InputBoxes(ctx)
  val outBoxObj = new OutBoxes(ctx)
  val compiler = new ContractCompile(ctx)

  val proxyAddress: Address = compiler
    .compileProxyContract(
      PhoenixContracts.phoenix_v1_hodlcoin_proxy.contractScript,
      serviceConf.minTxOperatorFee
    )
    .toAddress

  val minBoxValue = 1000000L
  val minerFee = 1000000L
  val minTxOperatorFee = 1000000L

  val inputs: Seq[InputBox] = inputBoxesObj.getInputs(
    Array(2 * Parameters.OneErg),
    txHelper.senderAddress
  )

  private val proxyOutput = outBoxObj.proxyMintInputBox(
    proxyAddress.toErgoContract,
    txHelper.senderAddress,
    new ErgoToken(contractConf.Contracts.bankContract.bankSingleton, 1L),
    new ErgoToken(contractConf.Contracts.bankContract.hodlCoin, 1L),
    minBoxValue,
    minerFee,
    Parameters.OneErg + minBoxValue + minerFee + minTxOperatorFee
  )

  private val unsignedTx = txHelper.buildUnsignedTransaction(
    inputs = inputs,
    outputs = Array(proxyOutput),
    fee = minerFee
  )

  val signedTx: SignedTransaction = txHelper.signTransaction(unsignedTx)

  val txId: String = txHelper.sendTx(signedTx)

  println(txId)
}

object sendToProxyMultiMint extends App {

  private val amountOfTxns = 1000
  private val amountHodlERGPerTxn = (1 * math.pow(10, 9)).toInt

  private val client: Client = new Client()
  client.setClient
  val ctx: BlockchainContext = client.getContext
  val serviceFilePath = "serviceOwner.json"
  val contractConfFilePath = "contracts.json"
  val serviceConf: ServiceOwnerConfig = serviceOwnerConf.read(serviceFilePath)
  private val contractConf = conf.read(contractConfFilePath)

  val walletMnemonic: String = serviceConf.txOperatorMnemonic
  val walletMnemonicPw: String = serviceConf.txOperatorMnemonicPw
  val txHelper =
    new TransactionHelper(this.ctx, walletMnemonic, walletMnemonicPw)

  val inputBoxesObj = new InputBoxes(ctx)
  val outBoxObj = new OutBoxes(ctx)
  val compiler = new ContractCompile(ctx)

  val proxyAddress: Address = compiler
    .compileProxyContract(
      PhoenixContracts.phoenix_v1_hodlcoin_proxy.contractScript,
      serviceConf.minTxOperatorFee
    )
    .toAddress

  val minBoxValue = 1000000L
  val minerFee = 1000000L
  val minTxOperatorFee = 1000000L

  private val totalFeesPerTx = minBoxValue + minerFee + minTxOperatorFee

  private val totalAmountPerTx =
    totalFeesPerTx + (amountHodlERGPerTxn)

  var inputs = inputBoxesObj.getInputs(
    Array(amountOfTxns * totalAmountPerTx),
    txHelper.senderAddress
  )

  for (i <- 1 to amountOfTxns) {

    val proxyOutput = outBoxObj.proxyMintInputBox(
      proxyAddress.toErgoContract,
      txHelper.senderAddress,
      new ErgoToken(contractConf.Contracts.bankContract.bankSingleton, 1L),
      new ErgoToken(contractConf.Contracts.bankContract.hodlCoin, 1L),
      minBoxValue,
      minerFee,
      totalAmountPerTx
    )

    val unsignedTx = txHelper.buildUnsignedTransaction(
      inputs = inputs,
      outputs = Array(proxyOutput),
      fee = minerFee
    )

    val signedTx: SignedTransaction = txHelper.signTransaction(unsignedTx)
    Thread.sleep(500)
    val txId: String = txHelper.sendTx(signedTx)

    println("Tx #" + i + ": " + txId)
    inputs = Seq(signedTx.getOutputsToSpend.get(2))
  }
}

object sendToProxyBurn extends App {
  private val client: Client = new Client()
  client.setClient
  val ctx: BlockchainContext = client.getContext
  val serviceFilePath = "serviceOwner.json"
  val contractConfFilePath = "contracts.json"
  val serviceConf: ServiceOwnerConfig = serviceOwnerConf.read(serviceFilePath)
  private val contractConf = conf.read(contractConfFilePath)

  val walletMnemonic: String = serviceConf.txOperatorMnemonic
  val walletMnemonicPw: String = serviceConf.txOperatorMnemonicPw
  val txHelper =
    new TransactionHelper(this.ctx, walletMnemonic, walletMnemonicPw)

  val inputBoxesObj = new InputBoxes(ctx)
  val outBoxObj = new OutBoxes(ctx)
  val compiler = new ContractCompile(ctx)

  val proxyAddress: Address = compiler
    .compileProxyContract(
      PhoenixContracts.phoenix_v1_hodlcoin_proxy.contractScript,
      serviceConf.minTxOperatorFee
    )
    .toAddress

  val minBoxValue = 1000000L
  val minerFee = 1000000L
  val minTxOperatorFee = 1000000L

  private val totalFees = minBoxValue + minerFee + minTxOperatorFee

  val hodlToken =
    new ErgoToken(contractConf.Contracts.bankContract.hodlCoin, 1L)

  val inputs: Seq[InputBox] = inputBoxesObj.getInputs(
    Array(minBoxValue + totalFees),
    txHelper.senderAddress,
    Array(hodlToken)
  )

  private val proxyOutput = outBoxObj.proxyBurnInputBox(
    proxyAddress.toErgoContract,
    txHelper.senderAddress,
    new ErgoToken(contractConf.Contracts.bankContract.bankSingleton, 1L),
    hodlToken,
    minBoxValue,
    minerFee,
    minBoxValue + totalFees
  )

  private val unsignedTx = txHelper.buildUnsignedTransaction(
    inputs = inputs,
    outputs = Array(proxyOutput),
    fee = minerFee
  )

  val signedTx: SignedTransaction = txHelper.signTransaction(unsignedTx)

  val txId: String = txHelper.sendTx(signedTx)

  println(txId)
}

object sendToProxyMultiBurn extends App {

  private val amountOfTxns = 200
  private val amountHodlERGPerTxn = (1 * math.pow(10, 9)).toInt

  private val client: Client = new Client()
  client.setClient
  val ctx: BlockchainContext = client.getContext
  val serviceFilePath = "serviceOwner.json"
  val contractConfFilePath = "contracts.json"
  val serviceConf: ServiceOwnerConfig = serviceOwnerConf.read(serviceFilePath)
  private val contractConf = conf.read(contractConfFilePath)

  val walletMnemonic: String = serviceConf.txOperatorMnemonic
  val walletMnemonicPw: String = serviceConf.txOperatorMnemonicPw
  val txHelper =
    new TransactionHelper(this.ctx, walletMnemonic, walletMnemonicPw)

  val inputBoxesObj = new InputBoxes(ctx)
  val outBoxObj = new OutBoxes(ctx)
  val compiler = new ContractCompile(ctx)

  val proxyAddress: Address = compiler
    .compileProxyContract(
      PhoenixContracts.phoenix_v1_hodlcoin_proxy.contractScript,
      serviceConf.minTxOperatorFee
    )
    .toAddress

  val minBoxValue = 1000000L
  val minerFee = 1000000L
  val minTxOperatorFee = 1000000L

  private val totalFeesPerTx = minerFee + minTxOperatorFee

  val hodlToken =
    new ErgoToken(
      contractConf.Contracts.bankContract.hodlCoin,
      amountHodlERGPerTxn.toLong * amountOfTxns.toLong
    )

  var inputs = inputBoxesObj.getInputs(
    Array(amountOfTxns * totalFeesPerTx),
    txHelper.senderAddress,
    Array(hodlToken)
  )


  for (i <- 1 to amountOfTxns) {

    val proxyOutput = outBoxObj.proxyBurnInputBox(
      proxyAddress.toErgoContract,
      txHelper.senderAddress,
      new ErgoToken(contractConf.Contracts.bankContract.bankSingleton, 1L),
      new ErgoToken(
        contractConf.Contracts.bankContract.hodlCoin,
        amountHodlERGPerTxn.toLong
      ),
      minBoxValue,
      minerFee,
      totalFeesPerTx
    )

    val unsignedTx = txHelper.buildUnsignedTransaction(
      inputs = inputs,
      outputs = Array(proxyOutput),
      fee = minerFee
    )

    val signedTx: SignedTransaction = txHelper.signTransaction(unsignedTx)
    Thread.sleep(500)
    val txId: String = txHelper.sendTx(signedTx)

    println("Tx #" + i + ": " + txId)
    inputs = Seq(signedTx.getOutputsToSpend.get(2))
  }
}
