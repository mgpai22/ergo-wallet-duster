package utils

import org.ergoplatform.appkit._
import scorex.crypto.hash.Blake2b256

class ContractCompile(ctx: BlockchainContext) {

  def compileDummyContract(
      contract: String = "sigmaProp(true)"
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder.empty(),
      contract
    )
  }
  def compileProxyContract(
      contract: String,
      minTxOperatorFee: Long
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item(
          "$minTxOperatorFee",
          minTxOperatorFee
        )
        .build(),
      contract
    )
  }

  def compileBankContract(
      contract: String,
      developerFeeContract: ErgoContract
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item(
          "$phoenixFeeContractBytesHash",
          Blake2b256(developerFeeContract.toAddress.asP2S().scriptBytes)
        )
        .build(),
      contract
    )
  }

  def compileFeeContract(
      contract: String,
      minMinerFeeNanoErg: Long
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item(
          "$minerFee",
          minMinerFeeNanoErg
        )
        .build(),
      contract
    )
  }

}
