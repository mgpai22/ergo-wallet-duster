package mockchain

import contracts.PhoenixContracts
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoContract,
  InputBox
}
import utils.{ContractCompile, OutBoxes, TransactionHelper}
import mockClient.{Common, HttpClientTesting}
import org.ergoplatform.sdk.ErgoToken
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PhoenixSpec
    extends AnyFlatSpec
    with Matchers
    with Common
    with PhoenixCommon {

  "PhoenixMintOperation" should "work correctly when all conditions are satisfied" in {

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

    val price = hodlPrice(hodlBox)

    require(
      hodlBox.getValue >= totalSupply - hodlErgAmount,
      "never-decreasing theorem does not hold"
    )
    require(
      price == 2000000,
      "Price does not correspond to manually calculated value"
    )

    val ergMintAmount = mintAmount(hodlBox, hodlMintAmount)
    require(
      ergMintAmount == 40,
      "Erg delta does not correspond to manually calculated value "
    )

    val fundingBox = outBoxObj
      .genericContractBox(compiler.compileDummyContract(), fundingBoxValue)
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
      new ErgoToken(hodlTokenId, hodlMintAmount),
      ergAmount
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, fundingBox),
      outputs = Array(hodlOutBox, recipientBox)
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }
  }

  "PhoenixMintOperation" should "fail if registers are changed" in {

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

    val fundingBox = outBoxObj
      .genericContractBox(compiler.compileDummyContract(), fundingBoxValue)
      .convertToInputWith(fakeTxId1, fakeIndex)

    val hodlOutBox = outBoxObj.hodlBankBox(
      phoenixContract,
      hodlSingleton,
      new ErgoToken(hodlTokenId, hodlErgAmount - hodlMintAmount),
      totalSupply - 1,
      precisionFactor + 10,
      minBankValue + 2,
      bankFee - 1,
      devFee + 5,
      ergAmount + ergMintAmount
    )

    val recipientBox = outBoxObj.hodlMintBox(
      userAddress,
      new ErgoToken(hodlTokenId, hodlMintAmount),
      ergAmount
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, fundingBox),
      outputs = Array(hodlOutBox, recipientBox)
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"
  }

  "PhoenixMintOperation" should "fail when insufficient ERGs are provided" in {

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

    val ergMintAmount =
      mintAmount(
        hodlBox,
        hodlMintAmount
      ) - 1 // this line changed - 1 nanoERG less to the bank

    val fundingBox = outBoxObj
      .genericContractBox(compiler.compileDummyContract(), fundingBoxValue)
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
      new ErgoToken(hodlTokenId, hodlMintAmount),
      ergAmount
    )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, fundingBox),
      outputs = Array(hodlOutBox, recipientBox)
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"
  }

  "PhoenixMintOperation" should "fail when excess HODL is taken" in {

    val ergAmount = 10000000 * 1000000000L
    val hodlErgAmount = totalSupply / 10 * 9

    val hodlMintAmount =
      hodlErgAmount + 1 // this line changed - 1 hodl more from the bank

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

    val ergMintAmount = mintAmount(
      hodlBox,
      hodlMintAmount
    ) // this line changed - still old amount of ERG paid

    val fundingBox = outBoxObj
      .genericContractBox(compiler.compileDummyContract(), fundingBoxValue)
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

    val recipientBox = outBoxObj
      .hodlMintBox(
        userAddress,
        new ErgoToken(hodlTokenId, hodlMintAmount),
        ergAmount
      )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, fundingBox),
      outputs = Array(hodlOutBox, recipientBox)
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "requirement failed: All token values should be > 0: "

  }

  "PhoenixBurnOperation" should "succeed when all conditions are met" in {

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

    val (userBoxAmount, devFeeAmount, bankFeeAmount) = burnAmount(hodlBox, hodlBurnAmount)
    val fundingBox = outBoxObj
      .tokenOutBox(
        Array(new ErgoToken(hodlTokenId, hodlBurnAmount)),
        compiler.compileDummyContract().toAddress,
        fundingBoxValue
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
      inputs = Array(hodlBox, fundingBox),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox)
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "PhoenixBurnOperation" should "fail when user's box takes more Erg than allowed" in {

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

    val (userBoxAmount, devFeeAmount, bankFeeAmount) = burnAmount(hodlBox, hodlBurnAmount)
    val fundingBox = outBoxObj
      .tokenOutBox(
        Array(new ErgoToken(hodlTokenId, hodlBurnAmount)),
        compiler.compileDummyContract().toAddress,
        fundingBoxValue
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
      ergAmount - userBoxAmount - devFeeAmount - 1 // <-- this line changed
    )

    val recipientBox = outBoxObj.simpleOutBox(
      userAddress,
      userBoxAmount + 1 // <-- this line changed
    )

    val devFeeBox =
      outBoxObj.simpleOutBox(feeContract.toAddress, devFeeAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, fundingBox),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox)
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "PhoenixBurnOperation" should "fail when developer's box takes more Erg than allowed" in {

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

    val (userBoxAmount, devFeeAmount, bankFeeAmount) = burnAmount(hodlBox, hodlBurnAmount)
    val fundingBox = outBoxObj
      .tokenOutBox(
        Array(new ErgoToken(hodlTokenId, hodlBurnAmount)),
        compiler.compileDummyContract().toAddress,
        fundingBoxValue
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
      userAddress,
      userBoxAmount
    )

    val devFeeBox =
      outBoxObj.simpleOutBox(
        feeContract.toAddress,
        devFeeAmount + 1 // <-- this line changed
      )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, fundingBox),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox)
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "PhoenixBurnOperation" should "fail when developer's box has an incorrect script" in {

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

    val (userBoxAmount, devFeeAmount, bankFeeAmount) = burnAmount(hodlBox, hodlBurnAmount)
    val fundingBox = outBoxObj
      .tokenOutBox(
        Array(new ErgoToken(hodlTokenId, hodlBurnAmount)),
        compiler.compileDummyContract().toAddress,
        fundingBoxValue
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
      outBoxObj.simpleOutBox(
        userAddress,
        devFeeAmount
      ) // <-- this line changed

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(hodlBox, fundingBox),
      outputs = Array(hodlOutBox, recipientBox, devFeeBox)
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

}
