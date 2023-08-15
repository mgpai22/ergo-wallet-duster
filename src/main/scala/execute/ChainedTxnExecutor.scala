package execute

import execute.HodlCalulations.{extractPrecisionFactor, hodlPrice}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoContract,
  InputBox
}
import org.ergoplatform.sdk.ErgoToken
import utils.{OutBoxes, TransactionHelper}

import scala.util.control.Breaks.{break, breakable}

object ChainedTxnExecutor {
  def main(
      bankBox: InputBox,
      inputs: Seq[InputBox],
      feeContract: ErgoContract,
      ctx: BlockchainContext,
      txHelper: TransactionHelper,
      dryRun: Boolean = false
  ): Unit = {

    val outBoxObj = new OutBoxes(ctx)

    val ergAmount = inputs.map(_.getValue).sum

    val startingHodlMintAmount = (((ergAmount - 2000000L).toDouble / mintAmount(
      hodlPrice(bankBox),
      oneBillionLong
    ).toDouble) * oneBillionLong).toLong

    val startingErgMintAmount =
      mintAmount(hodlPrice(bankBox), startingHodlMintAmount)

    var mutHodlBox = bankBox
    var mutFundingBox: InputBox = null

    var mutHodlMintAmount = startingHodlMintAmount
    var mutErgMintAmount = startingErgMintAmount

    val phoenixContract = Address
      .fromPropositionBytes(
        ctx.getNetworkType,
        bankBox.toErgoValue.getValue.propositionBytes.toArray
      )
      .toErgoContract

    val totalTokenSupply =
      bankBox.getRegisters.get(0).getValue.asInstanceOf[Long]
    val precisionFactor =
      bankBox.getRegisters.get(1).getValue.asInstanceOf[Long]
    val minBankValue =
      bankBox.getRegisters.get(2).getValue.asInstanceOf[Long]
    val devFee =
      bankBox.getRegisters.get(3).getValue.asInstanceOf[Long]
    val bankFee =
      bankBox.getRegisters.get(4).getValue.asInstanceOf[Long]

    var cnt = 0
    var mintCnt = 0
    var burnCnt = 0
    var balance = ergAmount

    breakable {
      while (balance > 1000000L) {

        try {

          val mintingHodlBox = outBoxObj.hodlBankBox(
            phoenixContract,
            mutHodlBox.getTokens.get(0),
            ErgoToken(
              mutHodlBox.getTokens.get(1).getId,
              mutHodlBox.getTokens.get(1).getValue - mutHodlMintAmount
            ),
            totalTokenSupply,
            precisionFactor,
            minBankValue,
            bankFee,
            devFee,
            mutHodlBox.getValue + mutErgMintAmount
          )

          val mintRecipientBox = outBoxObj.hodlMintBox(
            txHelper.senderAddress,
            ErgoToken(mutHodlBox.getTokens.get(1).getId, mutHodlMintAmount),
            1000000L
          )

          val unsignedMintTransaction = {
            if (cnt == 0) {
              txHelper.buildUnsignedTransaction(
                inputs = mutHodlBox +: inputs,
                outputs = Array(mintingHodlBox, mintRecipientBox)
              )
            } else {
              txHelper.buildUnsignedTransaction(
                inputs = Array(mutHodlBox, mutFundingBox),
                outputs = Array(mintingHodlBox, mintRecipientBox)
              )
            }
          }

          val signedMintTransaction =
            txHelper.signTransaction(unsignedMintTransaction)

          mintCnt += 1

          if (!dryRun) {
            val txHash = txHelper.sendTx(signedMintTransaction)
            println("mint: " + txHash + " " + mintCnt)
          } else {
            println("mint: " + mintCnt)
          }

          mutHodlBox = signedMintTransaction.getOutputsToSpend.get(0)
          mutFundingBox = signedMintTransaction.getOutputsToSpend.get(1)

          val (userBoxAmount, devFeeAmount, bankFeeAmount) =
            burnAmount(mutHodlBox, mutFundingBox.getTokens.get(0).getValue)

          val hodlBurnOutBox = outBoxObj.hodlBankBox(
            phoenixContract,
            mutHodlBox.getTokens.get(0),
            ErgoToken(
              mutHodlBox.getTokens.get(1).getId,
              mutHodlBox.getTokens
                .get(1)
                .getValue + mutFundingBox.getTokens.get(0).getValue
            ),
            totalTokenSupply,
            precisionFactor,
            minBankValue,
            bankFee,
            devFee,
            mintingHodlBox.getValue - userBoxAmount - devFeeAmount
          )

          val recipientBurnBox = outBoxObj.simpleOutBox(
            txHelper.senderAddress,
            userBoxAmount
          )

          val devFeeBox =
            outBoxObj.simpleOutBox(feeContract.toAddress, devFeeAmount)

          val unsignedBurnTransaction = txHelper.buildUnsignedTransaction(
            inputs = Array(mutHodlBox, mutFundingBox),
            outputs = Array(hodlBurnOutBox, recipientBurnBox, devFeeBox)
          )

          val signedBurnTransaction =
            txHelper.signTransaction(unsignedBurnTransaction)

          burnCnt += 1

          if (!dryRun) {
            val txHash = txHelper.sendTx(signedBurnTransaction)
            println("burn: " + txHash + " " + burnCnt)
          } else {
            println("burn: " + burnCnt)
          }

          mutHodlBox = signedBurnTransaction.getOutputsToSpend.get(0)
          mutFundingBox = signedBurnTransaction.getOutputsToSpend.get(1)
          balance = mutFundingBox.getValue

          mutErgMintAmount = mutFundingBox.getValue - 2000000L

          mutHodlMintAmount = ((mutErgMintAmount.toDouble / mintAmount(
            hodlPrice(mutHodlBox),
            oneBillionLong
          ).toDouble) * oneBillionLong).toLong

          if (!dryRun) {
            Thread.sleep(500)
          }

        } catch {
          case e: Exception => println(e); break
        }
        cnt += 1

      }
    }

    println("total mint transactions: " + mintCnt)
    println("total burn transactions: " + burnCnt)
    println("total transactions: " + cnt)

  }

  private def burnAmount(
      hodlBoxIn: InputBox,
      hodlBurnAmt: Long
  ): (Long, Long, Long) = {
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

  private def mintAmount(price: Long, hodlMintAmnt: Long): Long = {
    (hodlMintAmnt * price) / precisionFactor
  }

  // Constants

  private val precisionFactor = 1000000L
  private val oneBillionLong = 1000000000L
}
