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

  val brunoAddress: Address = Address.create(
    "9exfustUCPDKXsfDrGNrmtkyLDwAie2rKKdUsPVa26RuBFaYeCL" // revert back to original address
  )
  val pulsarzAddress: Address =
    Address.create("9hHondX3uZMY2wQsXuCGjbgZUqunQyZCNNuwGu6rL7AJC8dhRGa")
  val kushtiAddress: Address =
    Address.create("9iE2MadGSrn1ivHmRZJWRxzHffuAk6bPmEv6uJmPHuadBY8td5u")
  val krasAddress: Address = Address.create("9i9RhfdHQA2bHA8GqWKkYevp3nozASRjJfFkh29utjNL9gqE7Q7")
  val phoenixAddress: Address =
    Address.create("9iPs1ujGj2eKXVg82aGyAtUtQZQWxFaki48KFixoaNmUAoTY6wV")

  val feeContractAmount: Long = (100 * math.pow(10, 9)).toLong

  def getDevBoxes(totalAmountForDevs: Long): Array[OutBox] = {
    Array(
      outBoxObj.simpleOutBox(brunoAddress, (brunoNum * totalAmountForDevs)/feeDenom),
      outBoxObj.simpleOutBox(pulsarzAddress, (pulsarzNum * totalAmountForDevs)/feeDenom),
      outBoxObj.simpleOutBox(phoenixAddress, (phoenixNum * totalAmountForDevs)/feeDenom),
      outBoxObj.simpleOutBox(kushtiAddress, (kushtiNum * totalAmountForDevs)/feeDenom),
      outBoxObj.simpleOutBox(krasAddress, (krasNum * totalAmountForDevs)/feeDenom)
    )
  }

  "FeeContractWithdrawal" should "work correctly when all conditions are satisfied" in {

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val devAmount = feeContractInput.getValue - minMinerFeeNanoErg

    val devBoxes = getDevBoxes(devAmount)


    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes,
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

    val devBoxes = getDevBoxes(devAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes,
      fee = highMinerFee
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

  "FeeContractWithdrawal" should "fail with incorrect allocation" in {

    def getDevBoxes(totalAmountForDevs: Long): Array[OutBox] = {

      val brunoNum = 20L // <-- this changed
      val kushtiNum = 20L // <-- this changed
      Array(
        outBoxObj.simpleOutBox(brunoAddress, (brunoNum * totalAmountForDevs) / feeDenom),
        outBoxObj.simpleOutBox(pulsarzAddress, (pulsarzNum * totalAmountForDevs) / feeDenom),
        outBoxObj.simpleOutBox(phoenixAddress, (phoenixNum * totalAmountForDevs) / feeDenom),
        outBoxObj.simpleOutBox(kushtiAddress, (kushtiNum * totalAmountForDevs) / feeDenom),
        outBoxObj.simpleOutBox(krasAddress, (krasNum * totalAmountForDevs) / feeDenom)
      )
    }

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val devAmount = feeContractInput.getValue - minMinerFeeNanoErg

    val devBoxes = getDevBoxes(devAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes,
      fee = minMinerFeeNanoErg
    )

    the[Exception] thrownBy {
      txHelper.signTransaction(unsignedTransaction)
    } should have message "Script reduced to false"

  }

  "FeeContractWithdrawal" must "fail when the wrong address is used for receiving" in {

    def getDevBoxes(totalAmountForDevs: Long): Array[OutBox] = {

      val brunoAddress = Address.create("9fVNf9XCUitP8GDZk7gitfxMetQQdsyV5QxTbBo3AKWW5BJxSkn")

      Array(
        outBoxObj.simpleOutBox(brunoAddress, (brunoNum * totalAmountForDevs) / feeDenom),
        outBoxObj.simpleOutBox(pulsarzAddress, (pulsarzNum * totalAmountForDevs) / feeDenom),
        outBoxObj.simpleOutBox(phoenixAddress, (phoenixNum * totalAmountForDevs) / feeDenom),
        outBoxObj.simpleOutBox(kushtiAddress, (kushtiNum * totalAmountForDevs) / feeDenom),
        outBoxObj.simpleOutBox(krasAddress, (krasNum * totalAmountForDevs) / feeDenom)
      )
    }

    val feeContractInput =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, feeContractAmount)
        .convertToInputWith(fakeTxId1, fakeIndex)

    val devAmount = feeContractInput.getValue - minMinerFeeNanoErg

    val devBoxes = getDevBoxes(devAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes,
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

    val devBoxes = getDevBoxes(devAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes,
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

    val devBoxes = getDevBoxes(devAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput),
      outputs = devBoxes,
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
        .simpleOutBox(feeContract.toAddress, 1000000L)
        .convertToInputWith(fakeTxId2, fakeIndex)

    val feeContractInput3 =
      outBoxObj
        .simpleOutBox(feeContract.toAddress, 6969696960L)
        .convertToInputWith(fakeTxId3, fakeIndex)

    val devAmount =
      (feeContractInput.getValue + feeContractInput2.getValue + feeContractInput3.getValue) - minMinerFeeNanoErg

    val devBoxes = getDevBoxes(devAmount)

    val unsignedTransaction = txHelper.buildUnsignedTransaction(
      inputs = Array(feeContractInput, feeContractInput2, feeContractInput3),
      outputs = devBoxes,
      fee = minMinerFeeNanoErg
    )

    noException shouldBe thrownBy {
      txHelper.signTransaction(
        unsignedTransaction
      )
    }

  }

}
