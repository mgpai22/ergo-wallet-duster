package mockchain

import org.ergoplatform.appkit.{Address, OutBox}
import mockClient.{Common, HttpClientTesting}
import org.ergoplatform.sdk.ErgoToken
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.TransactionHelper

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class FeeSpec
    extends AnyFlatSpec
    with Matchers
    with HttpClientTesting
    with Common
    with PhoenixCommon {

  // mnemonic that belongs to one in contract
  // address: 9exfustUCPDKXsfDrGNrmtkyLDwAie2rKKdUsPVa26RuBFaYeCL
  override val txHelper = new TransactionHelper(
    ctx,
    "domain pencil motor legend high nurse grief degree anger pitch invite elite virus swift pottery",
    ""
  )

  val dev1Address: Address = Address.create(
    "9exfustUCPDKXsfDrGNrmtkyLDwAie2rKKdUsPVa26RuBFaYeCL"
  ) // revert back to original address
  val dev2Address: Address =
    Address.create("9gnBtmSRBMaNTkLQUABoAqmU2wzn27hgqVvezAC9SU1VqFKZCp8")
  val dev3Address: Address =
    Address.create("9iE2MadGSrn1ivHmRZJWRxzHffuAk6bPmEv6uJmPHuadBY8td5u")
  val phoenixAddress: Address =
    Address.create("9iPs1ujGj2eKXVg82aGyAtUtQZQWxFaki48KFixoaNmUAoTY6wV")

  val feeContractAmount: Long = (100 * math.pow(10, 9)).toLong

  def getDevBoxes(amountPerDev: Long): Array[OutBox] = {
    Array(
      outBoxObj.simpleOutBox(dev1Address, amountPerDev),
      outBoxObj.simpleOutBox(dev2Address, amountPerDev),
      outBoxObj.simpleOutBox(dev3Address, amountPerDev)
    )
  }

  "FeeContractWithdrawal" should "work correctly when all conditions are satisfied" in {

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val devAmount = feeContractInput.getValue - minMinerFeeNanoErg
    val devAllocation =
      ((devPercentNumerator * devAmount) / percentDenominator) / 3L
    val devBoxes = getDevBoxes(devAllocation)

    val phoenixAllocation =
      (phoenixPercentNumerator * devAmount) / percentDenominator
    val phoenixBox = outBoxObj.simpleOutBox(phoenixAddress, phoenixAllocation)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes :+ phoenixBox,
      fee = minMinerFeeNanoErg
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "FeeContractWithdrawal" must "operate correctly when all conditions are met, even with a high miner fee" in {

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val highMinerFee = 100000000L

    val devAmount = feeContractInput.getValue - highMinerFee
    val devAllocation =
      ((devPercentNumerator * devAmount) / percentDenominator) / 3L
    val devBoxes = getDevBoxes(devAllocation)

    val phoenixAllocation =
      (phoenixPercentNumerator * devAmount) / percentDenominator
    val phoenixBox = outBoxObj.simpleOutBox(phoenixAddress, phoenixAllocation)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes :+ phoenixBox,
      fee = highMinerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "FeeContractWithdrawal" should "fail with incorrect allocation" in {

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val devAmount = feeContractInput.getValue - minMinerFeeNanoErg
    val devAllocation =
      ((devPercentNumerator * devAmount) / percentDenominator) / 3L
    val devBoxes = getDevBoxes(devAllocation + 1L) // <-- this changed

    val phoenixAllocation =
      (phoenixPercentNumerator * devAmount) / percentDenominator
    val phoenixBox =
      outBoxObj.simpleOutBox(
        phoenixAddress,
        phoenixAllocation - 3L // <-- this changed
      )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes :+ phoenixBox,
      fee = minMinerFeeNanoErg
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "FeeContractWithdrawal" must "fail when the wrong address is used for receiving" in {

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val devAmount = feeContractInput.getValue - minMinerFeeNanoErg
    val devAllocation =
      ((devPercentNumerator * devAmount) / percentDenominator) / 3L
    val devBoxes = getDevBoxes(devAllocation)

    val phoenixAllocation =
      (phoenixPercentNumerator * devAmount) / percentDenominator
    val phoenixBox =
      outBoxObj.simpleOutBox(
        Address.create(
          "9gNYeyfRFUipiWZ3JR1ayDMoeh28E6J7aDQosb7yrzsuGSDqzCC" // <-- this changed
        ),
        phoenixAllocation
      )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes :+ phoenixBox,
      fee = minMinerFeeNanoErg
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "FeeContractWithdrawal" must "fail when dust is claimed" in {

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, 2100000L) // <-- this changed
        .convertToInputWith(fakeTxId1, fakeIndex)

    val devAmount = feeContractInput.getValue - minMinerFeeNanoErg
    val devAllocation =
      ((devPercentNumerator * devAmount) / percentDenominator) / 3L
    val devBoxes = getDevBoxes(devAllocation)

    val phoenixAllocation =
      (phoenixPercentNumerator * devAmount) / percentDenominator
    val phoenixBox =
      outBoxObj.simpleOutBox(
        phoenixAddress,
        phoenixAllocation
      )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes :+ phoenixBox,
      fee = minMinerFeeNanoErg
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "FeeContractWithdrawal" should "fail with incorrect signer" in {

    // random signer
    val txHelper = new TransactionHelper(
      ctx,
      "expire soon coral camp wing cross raccoon brick mango about sadness wine resist snake wire",
      ""
    )

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val devAmount = feeContractInput.getValue - minMinerFeeNanoErg
    val devAllocation =
      ((devPercentNumerator * devAmount) / percentDenominator) / 3L
    val devBoxes = getDevBoxes(devAllocation)

    val phoenixAllocation =
      (phoenixPercentNumerator * devAmount) / percentDenominator
    val phoenixBox =
      outBoxObj.simpleOutBox(
        phoenixAddress,
        phoenixAllocation
      )

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes :+ phoenixBox,
      fee = minMinerFeeNanoErg
    )

    the[AssertionError] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    }

  }

  "FeeContractWithdrawalWithMultipleInputs" should "work correctly when all conditions are satisfied" in {

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val feeContractInput2 =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, 100000L)
        .convertToInputWith(fakeTxId2, fakeIndex)

    val feeContractInput3 =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, 100004314250L)
        .convertToInputWith(fakeTxId3, fakeIndex)

    val devAmount =
      (feeContractInput.getValue + feeContractInput2.getValue + feeContractInput3.getValue) - minMinerFeeNanoErg
    val devAllocation =
      ((devPercentNumerator * devAmount) / percentDenominator) / 3L
    val devBoxes = getDevBoxes(devAllocation)

    val phoenixAllocation =
      (phoenixPercentNumerator * devAmount) / percentDenominator
    val phoenixBox = outBoxObj.simpleOutBox(phoenixAddress, phoenixAllocation)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput, feeContractInput2, feeContractInput3),
      outputs = devBoxes :+ phoenixBox,
      fee = minMinerFeeNanoErg
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

}
