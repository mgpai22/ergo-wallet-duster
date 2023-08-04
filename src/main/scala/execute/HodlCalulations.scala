package execute

import org.ergoplatform.appkit.InputBox

object HodlCalulations {

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

  def extractPrecisionFactor(hodlBoxIn: InputBox): Long = {
    hodlBoxIn.getRegisters.get(1).getValue.asInstanceOf[Long]
  }

  def hodlMintAmountFromERG(hodlBoxIn: InputBox, ergMintAmt: Long): BigInt = {
    val price = hodlPrice(hodlBoxIn)
    val precisionFactor = extractPrecisionFactor(hodlBoxIn)
    ergMintAmt * precisionFactor / price
  }

}
