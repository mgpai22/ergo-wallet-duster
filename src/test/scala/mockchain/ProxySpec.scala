package mockchain

import org.ergoplatform.appkit.{Address, OutBox, Parameters}
import mockClient.{Common, HttpClientTesting}
import org.ergoplatform.sdk.ErgoToken
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.TransactionHelper

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ProxySpec
    extends AnyFlatSpec
    with Matchers
    with HttpClientTesting
    with Common
    with PhoenixCommon {

  override val txHelper = new TransactionHelper(
    ctx,
    "domain pencil motor legend high nurse grief degree anger pitch invite elite virus swift pottery",
    ""
  )

  val minerFee = 1600000L
  val recommendedMinerFee: Long = 1000000L

  val hodlBankSingleton = new ErgoToken(hodlBankNft, 1L)
  val dummyHodlTokens = new ErgoToken(hodlTokenId, 100L)

  "PhoenixMintOperationWithProxy" should "work correctly when all conditions are satisfied" in {

    val ergAmount = 10000000 * 1000000000L
    val hodlErgAmount = totalSupply / 10 * 9
    val hodlMintAmount = 20

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalSupply,
        precisionFactor,
        minBankValue,
        bankFee,
        devFee,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val ergMintAmount = mintAmount(hodlBox, hodlMintAmount)

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount - hodlMintAmount),
      totalSupply,
      precisionFactor,
      minBankValue,
      bankFee,
      devFee,
      ergAmount + ergMintAmount
    )

    val recipientBox = outBoxObj.hodlMintBox(
      userAddress,
      new ErgoToken(hodlTokenId, hodlMintAmount)
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox),
      fee = minerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "PhoenixMintOperationWithProxyWithBuggyDecimals" should "fail since amountHodlToMint goes to zero" in {

    val ergAmount = 1009584710L
    val hodlErgAmount = 97739923L

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val totalTokenSupply = 97739924L
    val precisionFactor = 1000000L
    val minBankValue = 1000000L
    val bankFeeNum = 3L
    val devFeeNum = 1L

    val minerFee = 1000000L
    val minTxOperatorFee = 1000000L
    val minBoxValue = 1000000L

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalTokenSupply,
        precisionFactor,
        minBankValue,
        bankFeeNum,
        devFeeNum,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val ergMintAmount = Parameters.OneErg

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val amountHodlToMint = hodlMintAmount(hodlBox, ergMintAmount)

    println(amountHodlToMint)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount - amountHodlToMint),
      totalTokenSupply,
      precisionFactor,
      minBankValue,
      bankFeeNum,
      devFeeNum,
      ergAmount + ergMintAmount
    )

    val recipientBox = outBoxObj.hodlMintBox(
      userAddress,
      new ErgoToken(hodlTokenId, amountHodlToMint)
    )

    a[IllegalArgumentException] shouldBe thrownBy {

      val unsignedTransaction = txHelper.buildUnsignedTransaction(
        inputs = Array(hodlBox, proxyInput),
        outputs = Array(hodlOutBox, recipientBox),
        fee = minerFee
      )

      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "PhoenixMintOperationWithProxyWithBuggyDecimals" should "work correctly when hodlTokens get decimals" in {

    val ergAmount = 1009584710L
    val hodlErgAmount = (97739923 * math.pow(10, 9)).toLong

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val totalTokenSupply = (97739924 * math.pow(10, 9)).toLong
    val precisionFactor = 1000000L
    val minBankValue = 1000000L
    val bankFeeNum = 3L
    val devFeeNum = 1L

    val minerFee = 1000000L
    val minTxOperatorFee = 1000000L
    val minBoxValue = 1000000L

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalTokenSupply,
        precisionFactor,
        minBankValue,
        bankFeeNum,
        devFeeNum,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val ergMintAmount = Parameters.OneErg

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val amountHodlToMint = hodlMintAmount(hodlBox, ergMintAmount)

    println(amountHodlToMint)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount - amountHodlToMint),
      totalTokenSupply,
      precisionFactor,
      minBankValue,
      bankFeeNum,
      devFeeNum,
      ergAmount + ergMintAmount
    )

    val recipientBox = outBoxObj.hodlMintBox(
      userAddress,
      new ErgoToken(hodlTokenId, amountHodlToMint)
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox),
      fee = minerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "PhoenixMintOperationWithProxy" should "work correctly when all conditions are satisfied with a generous tx operator fee" in {

    val ergAmount = 10000000 * 1000000000L
    val hodlErgAmount = totalSupply / 10 * 9
    val hodlMintAmount = 20

    val generousTxOperatorFee = 1000000000L

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalSupply,
        precisionFactor,
        minBankValue,
        bankFee,
        devFee,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val ergMintAmount = mintAmount(hodlBox, hodlMintAmount)

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + generousTxOperatorFee // <-- this is changed
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount - hodlMintAmount),
      totalSupply,
      precisionFactor,
      minBankValue,
      bankFee,
      devFee,
      ergAmount + ergMintAmount
    )

    val recipientBox = outBoxObj.hodlMintBox(
      userAddress,
      new ErgoToken(hodlTokenId, hodlMintAmount)
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox),
      fee = minerFee
    )

    require(
      unsignedTransaction.getOutputs
        .get(3)
        .getValue == generousTxOperatorFee // <-- this is changed
      ,
      "Does not receive generous tx operator fee"
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "PhoenixMintOperationWithProxy" should "fail when offchain code does not use the specified miner fee" in {

    val ergAmount = 10000000 * 1000000000L
    val hodlErgAmount = totalSupply / 10 * 9
    val hodlMintAmount = 20

    val generousMinerFee = 1000000000L // <-- this is changed

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalSupply,
        precisionFactor,
        minBankValue,
        bankFee,
        devFee,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val ergMintAmount = mintAmount(hodlBox, hodlMintAmount)

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        generousMinerFee, // <-- this is changed
        ergMintAmount + minBoxValue + generousMinerFee + minTxOperatorFee // <-- this is changed
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount - hodlMintAmount),
      totalSupply,
      precisionFactor,
      minBankValue,
      bankFee,
      devFee,
      ergAmount + ergMintAmount
    )

    val recipientBox = outBoxObj.hodlMintBox(
      userAddress,
      new ErgoToken(hodlTokenId, hodlMintAmount)
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox),
      fee = minerFee // <-- offchain uses usual miner fee
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "PhoenixMintOperationWithProxy" should "fail when offchain code sends to an address which does not belong to buyer" in {

    val ergAmount = 10000000 * 1000000000L
    val hodlErgAmount = totalSupply / 10 * 9
    val hodlMintAmount = 20

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalSupply,
        precisionFactor,
        minBankValue,
        bankFee,
        devFee,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val ergMintAmount = mintAmount(hodlBox, hodlMintAmount)

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount - hodlMintAmount),
      totalSupply,
      precisionFactor,
      minBankValue,
      bankFee,
      devFee,
      ergAmount + ergMintAmount
    )

    val recipientBox = outBoxObj.hodlMintBox(
      Address.create(
        "9gNYeyfRFUipiWZ3JR1ayDMoeh28E6J7aDQosb7yrzsuGSDqzCC"
      ), // <-- this is changed
      new ErgoToken(hodlTokenId, hodlMintAmount)
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox),
      fee = minerFee
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "PhoenixBurnOperationWithProxy" should "succeed when all conditions are met" in {

    val ergAmount = 1000 * 1000000000L
    val hodlErgAmount = 100 * 1000000000L

    val hodlBurnAmount = 20

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalSupply,
        precisionFactor,
        minBankValue,
        bankFee,
        devFee,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val (userBoxAmount, devFeeAmount, bankFeeAmount) =
      burnAmount(hodlBox, hodlBurnAmount)

    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, hodlBurnAmount),
        minBoxValue,
        minerFee,
        minerFee + minTxOperatorFee // <-- note that minBoxValue is not needed
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount + hodlBurnAmount),
      totalSupply,
      precisionFactor,
      minBankValue,
      bankFee,
      devFee,
      ergAmount - userBoxAmount - devFeeAmount
    )

    val recipientBox = outBoxObj.simpleOutBox(userAddress, userBoxAmount)

    val devFeeBox =
      outBoxObj.simpleOutBox(feeContract.toAddress, devFeeAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox),
      fee = minerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "PhoenixBurnOperationWithProxyRealistic" should "succeed when all conditions are met" in {

    val ergAmount = 1159000000000L
    val hodlErgAmount = 97738765L

    val hodlBurnAmount = 1L

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val totalTokenSupply = 97739924L
    val precisionFactor = 1000000L
    val minBankValue = 1000000L
    val bankFeeNum = 3L
    val devFeeNum = 1L

    val minerFee = 1000000L
    val minTxOperatorFee = 1000000L
    val minBoxValue = 1000000L

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalTokenSupply,
        precisionFactor,
        minBankValue,
        bankFeeNum,
        devFeeNum,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val (expectedAmountWithdrawn, devFeeAmount, bankFeeAmount) =
      burnAmount(hodlBox, hodlBurnAmount)

    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, hodlBurnAmount),
        minBoxValue,
        minerFee,
        minerFee + minTxOperatorFee // <-- note that minBoxValue is not needed
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    println("Reserve In: " + ergAmount)
    println(
      "Reserve Out: " + (ergAmount - expectedAmountWithdrawn - devFeeAmount)
    )
    println("userBoxAmount: " + expectedAmountWithdrawn)
    println("devFeeAmount: " + devFeeAmount)
    println("bankFeeAmount: " + bankFeeAmount)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount + hodlBurnAmount),
      totalTokenSupply,
      precisionFactor,
      minBankValue,
      bankFeeNum,
      devFeeNum,
      ergAmount - expectedAmountWithdrawn - devFeeAmount
    )

    val recipientBox =
      outBoxObj.simpleOutBox(userAddress, expectedAmountWithdrawn)

    val devFeeBox =
      outBoxObj.simpleOutBox(feeContract.toAddress, devFeeAmount)

    Array(hodlOutBox, recipientBox, devFeeBox).foreach(o =>
      println("Output: " + o.getValue)
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox),
      fee = minerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "PhoenixBurnOperationWithProxy" should "succeed when all conditions are met with a generous tx operator fee" in {

    val ergAmount = 1000 * 1000000000L
    val hodlErgAmount = 100 * 1000000000L

    val generousTxOperatorFee = 1000000000L // <-- this is changed

    val hodlBurnAmount = 20

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalSupply,
        precisionFactor,
        minBankValue,
        bankFee,
        devFee,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val (userBoxAmount, devFeeAmount, bankFeeAmount) =
      burnAmount(hodlBox, hodlBurnAmount)
    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, hodlBurnAmount),
        minBoxValue,
        minerFee,
        minerFee + generousTxOperatorFee // <-- note that minBoxValue is not needed
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount + hodlBurnAmount),
      totalSupply,
      precisionFactor,
      minBankValue,
      bankFee,
      devFee,
      ergAmount - userBoxAmount - devFeeAmount
    )

    val recipientBox = outBoxObj.simpleOutBox(userAddress, userBoxAmount)

    val devFeeBox =
      outBoxObj.simpleOutBox(feeContract.toAddress, devFeeAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox),
      fee = minerFee
    )

    require(
      unsignedTransaction.getOutputs
        .get(4)
        .getValue == generousTxOperatorFee, // <-- this is changed
      "Does not receive generous tx operator fee"
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "PhoenixBurnOperationWithProxy" should "fail when offchain code does not use the specified miner fee" in {

    val ergAmount = 1000 * 1000000000L
    val hodlErgAmount = 100 * 1000000000L

    val generousMinerFee = 1000000000L

    val hodlBurnAmount = 20

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalSupply,
        precisionFactor,
        minBankValue,
        bankFee,
        devFee,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val (userBoxAmount, devFeeAmount, bankFeeAmount) =
      burnAmount(hodlBox, hodlBurnAmount)
    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, hodlBurnAmount),
        minBoxValue,
        generousMinerFee,
        generousMinerFee + minTxOperatorFee // <-- this is changed
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount + hodlBurnAmount),
      totalSupply,
      precisionFactor,
      minBankValue,
      bankFee,
      devFee,
      ergAmount - userBoxAmount - devFeeAmount
    )

    val recipientBox = outBoxObj.simpleOutBox(userAddress, userBoxAmount)

    val devFeeBox =
      outBoxObj.simpleOutBox(feeContract.toAddress, devFeeAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox),
      fee = minerFee // <-- note that the offchain uses the usual miner fee
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "PhoenixBurnOperationWithProxy" should "fail when offchain code sends to an address which does not belong to buyer" in {

    val ergAmount = 1000 * 1000000000L
    val hodlErgAmount = 100 * 1000000000L

    val hodlBurnAmount = 20

    val hodlSingleton = new ErgoToken(hodlBankNft, 1L)
    val hodlTokens = new ErgoToken(hodlTokenId, hodlErgAmount)

    val hodlBox = outBoxObj
      .hodlBankBox(
        phoenixContract,
        hodlSingleton,
        hodlTokens,
        totalSupply,
        precisionFactor,
        minBankValue,
        bankFee,
        devFee,
        ergAmount
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val (userBoxAmount, devFeeAmount, bankFeeAmount) =
      burnAmount(hodlBox, hodlBurnAmount)
    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, hodlBurnAmount),
        minerFee,
        minerFee,
        minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount + hodlBurnAmount),
      totalSupply,
      precisionFactor,
      minBankValue,
      bankFee,
      devFee,
      ergAmount - userBoxAmount - devFeeAmount
    )

    val recipientBox = outBoxObj.simpleOutBox(
      Address.create(
        "9gNYeyfRFUipiWZ3JR1ayDMoeh28E6J7aDQosb7yrzsuGSDqzCC" // <-- this is changed
      ),
      userBoxAmount
    )

    val devFeeBox =
      outBoxObj.simpleOutBox(feeContract.toAddress, devFeeAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, proxyInput),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox),
      fee = minerFee
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "ProxyRefund" should "work correctly when all conditions are satisfied" in {

    val ergMintAmount = 40L
    // note that the buyer has to sign their own refund tx
    val txHelper = new TransactionHelper(
      ctx,
      "pond trick believe salt obscure wool end state thing fringe reunion legend quarter popular oak",
      ""
    )
    val userAddress = txHelper.senderAddress

    val fundingBox = outBoxObj
      .simpleOutBox(userAddress, recommendedMinerFee)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val recipientBox = outBoxObj.simpleOutBox(
      userAddress,
      proxyInput.getValue
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(fundingBox, proxyInput),
      outputs = Array(recipientBox),
      fee = recommendedMinerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )

    }
  }

  "ProxyRefund" must "function properly irrespective of the input order, given all conditions are met" in {

    val ergMintAmount = 40L
    // note that the buyer has to sign their own refund tx
    val txHelper = new TransactionHelper(
      ctx,
      "pond trick believe salt obscure wool end state thing fringe reunion legend quarter popular oak",
      ""
    )
    val userAddress = txHelper.senderAddress

    val fundingBox = outBoxObj
      .simpleOutBox(userAddress, recommendedMinerFee)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val recipientBox = outBoxObj.simpleOutBox(
      userAddress,
      proxyInput.getValue
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(proxyInput, fundingBox),
      outputs = Array(recipientBox),
      fee = recommendedMinerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )

    }
  }

  "ProxyRefund" must "fail when signed by someone other than the buyer" in {

    val ergMintAmount = 40L

    val fundingBox = outBoxObj
      .simpleOutBox(userAddress, recommendedMinerFee)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val recipientBox = outBoxObj.simpleOutBox(
      userAddress,
      proxyInput.getValue
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(proxyInput, fundingBox),
      outputs = Array(recipientBox),
      fee = recommendedMinerFee
    )

    the[AssertionError] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    }

  }

  "ProxyRefundWithToken" should "work correctly when all conditions are satisfied" in {

    val ergMintAmount = 40L
    // note that the buyer has to sign their own refund tx
    val txHelper = new TransactionHelper(
      ctx,
      "pond trick believe salt obscure wool end state thing fringe reunion legend quarter popular oak",
      ""
    )
    val userAddress = txHelper.senderAddress

    val fundingBox = outBoxObj
      .simpleOutBox(userAddress, recommendedMinerFee)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val recipientBox = outBoxObj.hodlMintBox(
      userAddress,
      dummyHodlTokens
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(fundingBox, proxyInput),
      outputs = Array(recipientBox),
      fee = recommendedMinerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )

    }
  }

  "ProxyRefundWithMultipleInputs" should "work correctly when all conditions are satisfied" in {

    val ergMintAmount = 40L
    // note that the buyer has to sign their own refund tx
    val txHelper = new TransactionHelper(
      ctx,
      "pond trick believe salt obscure wool end state thing fringe reunion legend quarter popular oak",
      ""
    )
    val userAddress = txHelper.senderAddress

    val fundingBox = outBoxObj
      .simpleOutBox(userAddress, recommendedMinerFee)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput2 = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        100000L + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId2, fakeIndex)

    val proxyInput3 = outBoxObj
      .proxyMintInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        10041514300L + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId3, fakeIndex)

    val recipientBox = outBoxObj.simpleOutBox(
      userAddress,
      proxyInput.getValue + proxyInput2.getValue + proxyInput3.getValue
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(fundingBox, proxyInput, proxyInput2, proxyInput3),
      outputs = Array(recipientBox),
      fee = recommendedMinerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )

    }
  }

  "ProxyRefundWithMultipleInputsAndTokens" should "work correctly when all conditions are satisfied" in {

    val ergMintAmount = 40L
    // note that the buyer has to sign their own refund tx
    val txHelper = new TransactionHelper(
      ctx,
      "pond trick believe salt obscure wool end state thing fringe reunion legend quarter popular oak",
      ""
    )
    val userAddress = txHelper.senderAddress

    val fundingBox = outBoxObj
      .simpleOutBox(userAddress, recommendedMinerFee)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput2 = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, 47L),
        minBoxValue,
        minerFee,
        100000L + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId2, fakeIndex)

    val proxyInput3 = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, 52438924L),
        minBoxValue,
        minerFee,
        10041514300L + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId3, fakeIndex)

    val inputs = Array(fundingBox, proxyInput, proxyInput2, proxyInput3)

    val outputs = inputs.tail.map { i =>
      val recipientBox = outBoxObj.optionalTokenOutBox(
        i.getTokens.asScala,
        userAddress,
        i.getValue
      )
      recipientBox
    }

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = inputs,
      outputs = outputs,
      fee = recommendedMinerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )

    }
  }

  "ProxyRefundWithMultipleInputsAndTokens" should "fail when all tokens are not returned" in {

    val ergMintAmount = 40L
    // note that the buyer has to sign their own refund tx
    val txHelper = new TransactionHelper(
      ctx,
      "pond trick believe salt obscure wool end state thing fringe reunion legend quarter popular oak",
      ""
    )
    val userAddress = txHelper.senderAddress

    val fundingBox = outBoxObj
      .simpleOutBox(userAddress, recommendedMinerFee)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput2 = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, 47L),
        minBoxValue,
        minerFee,
        100000L + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId2, fakeIndex)

    val proxyInput3 = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, 52438924L),
        minBoxValue,
        minerFee,
        10041514300L + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId3, fakeIndex)

    val inputs = Array(fundingBox, proxyInput, proxyInput2, proxyInput3)

    val outputs = {
      val outputsList = new ListBuffer[OutBox]
      // remove token from third input so it can be burned
      inputs.tail.zipWithIndex.collect { case (input, index) =>
        val tokens: mutable.Buffer[ErgoToken] =
          if (index == 2) mutable.Buffer.empty[ErgoToken]
          else input.getTokens.asScala
        val recipientBox =
          outBoxObj.optionalTokenOutBox(tokens, userAddress, input.getValue)
        outputsList.append(recipientBox)
      }
      outputsList
    }

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = inputs,
      outputs = outputs,
      tokensToBurn = proxyInput3.getTokens.asScala,
      fee = recommendedMinerFee
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "ProxyRefundWithMultipleInputsAndDifferentTokens" should "work correctly when all conditions are satisfied" in {

    val ergMintAmount = 40L
    // note that the buyer has to sign their own refund tx
    val txHelper = new TransactionHelper(
      ctx,
      "pond trick believe salt obscure wool end state thing fringe reunion legend quarter popular oak",
      ""
    )
    val userAddress = txHelper.senderAddress

    val fundingBox = outBoxObj
      .simpleOutBox(userAddress, recommendedMinerFee)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        dummyHodlTokens,
        minBoxValue,
        minerFee,
        ergMintAmount + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId1, fakeIndex)

    val proxyInput2 = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(hodlTokenId, 47L),
        minBoxValue,
        minerFee,
        100000L + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId2, fakeIndex)

    val proxyInput3 = outBoxObj
      .proxyBurnInputBox(
        proxyContract,
        userAddress,
        hodlBankSingleton,
        new ErgoToken(dexyUSD, 52438924L),
        minBoxValue,
        minerFee,
        10041514300L + minBoxValue + minerFee + minTxOperatorFee
      )
      .convertToInputWith(fakeTxId3, fakeIndex)

    val inputs = Array(fundingBox, proxyInput, proxyInput2, proxyInput3)

    val outputs = inputs.tail.map { i =>
      val recipientBox = outBoxObj.optionalTokenOutBox(
        i.getTokens.asScala,
        userAddress,
        i.getValue
      )
      recipientBox
    }

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = inputs,
      outputs = outputs,
      fee = recommendedMinerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )

    }
  }

}
