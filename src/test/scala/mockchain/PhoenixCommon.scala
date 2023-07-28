package mockchain

import contracts.PhoenixContracts
import mockUtils.FileMockedErgoClient
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoContract,
  InputBox
}
import utils.{ContractCompile, OutBoxes, TransactionHelper}
import mockClient.HttpClientTesting

trait PhoenixCommon extends HttpClientTesting {

  val ergoClient: FileMockedErgoClient = createMockedErgoClient(
    MockData(Nil, Nil)
  )

  val ctx: BlockchainContext = ergoClient.execute(ctx => ctx)

  val compiler = new ContractCompile(ctx)
  val txHelper = new TransactionHelper(ctx, "", "")
  val outBoxObj = new OutBoxes(ctx)

  val percentDenominator = 100L
  val devPercentNumerator = 60L
  val phoenixPercentNumerator = 40L

  val minMinerFeeNanoErg = 1600000L

  val fundingBoxValue: Long = 50000000 * 1000000000L
  val minBoxValue = 1000000L
  val minTxOperatorFee = 1000000L

  val hodlTokenId =
    "2cbabc2be7292e2e857a1f2c34a8b0c090de2f30fa44c68ab71454e5586bd45e"
  val hodlBankNft =
    "2bbabc2be7292e2e857a1f2c34a8b0c090de2f30fa44c68ab71454e5586bd45e"

  val userAddress: Address =
    Address.create("9eiuh5bJtw9oWDVcfJnwTm1EHfK5949MEm5DStc2sD1TLwDSrpx")

  val feeScript: String =
    PhoenixContracts.phoenix_v1_hodlcoin_feeTest_mainnet.contractScript

  val phoenixScript: String =
    PhoenixContracts.phoenix_v1_hodlcoin_bank.contractScript

  val feeContract: ErgoContract = compiler.compileFeeContract(
    feeScript,
    minMinerFeeNanoErg
  )

  val phoenixContract: ErgoContract =
    compiler.compileBankContract(phoenixScript, feeContract)

  val proxyScript: String =
    PhoenixContracts.phoenix_v1_hodlcoin_proxy.contractScript
  val proxyContract: ErgoContract =
    compiler.compileProxyContract(proxyScript, minTxOperatorFee)

  // R4: Long             TotalTokenSupply
  // R5: Long             PrecisionFactor
  // R6: Long             MinBankValue
  // R7: Long             BankFee
  // R8: Long             DevFee

  val totalSupply: Long = 50000000 * 1000000000L
  val precisionFactor = 1000000L
  val minBankValue = 1000000L
  val bankFee = 30L
  val devFee = 3L

  def extractPrecisionFactor(hodlBoxIn: InputBox): Long = {
    val precisionFactor =
      hodlBoxIn.getRegisters.get(1).getValue.asInstanceOf[Long] // R5
    precisionFactor
  }

  def hodlPrice(hodlBoxIn: InputBox): Long = {
    // preserving terminology from the contract
    val reserveIn = hodlBoxIn.getValue
    val totalTokenSupply =
      hodlBoxIn.getRegisters.get(0).getValue.asInstanceOf[Long] // R4
    val hodlCoinsIn: Long = hodlBoxIn.getTokens.get(1).getValue
    val hodlCoinsCircIn: Long = totalTokenSupply - hodlCoinsIn
    val precisionFactor = extractPrecisionFactor(hodlBoxIn)
    ((BigInt(reserveIn) * BigInt(precisionFactor)) / BigInt(
      hodlCoinsCircIn
    )).toLong
  }

  // amount of (nano) ERGs needed to mint given amount of hodlcoins against given hodl bank
  def mintAmount(hodlBoxIn: InputBox, hodlMintAmt: Long): Long = {
    val price = hodlPrice(hodlBoxIn)
    val precisionFactor = extractPrecisionFactor(hodlBoxIn)
    hodlMintAmt * price / precisionFactor
  }

  /** @return amount of hodl tokens to mint give amount of ERGs paid
    */
  def hodlMintAmount(hodlBoxIn: InputBox, ergMintAmt: Long): Long = {
    val price = hodlPrice(hodlBoxIn)
    val precisionFactor = extractPrecisionFactor(hodlBoxIn)
    ergMintAmt * precisionFactor / price
  }

  // amount of (nano) ERGs which can be released to when given amount of hodlcoins burnt

  /** @return amount of (nano) ERGs which can be released to when given amount of hodlcoins burnt to user,
    *         and also dev fee
    */
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

  def hodlMintAmountFromERG(hodlBoxIn: InputBox, ergMintAmt: Long): Long = {
    val price = hodlPrice(hodlBoxIn)
    val precisionFactor = extractPrecisionFactor(hodlBoxIn)
    ergMintAmt * precisionFactor / price
  }

}
