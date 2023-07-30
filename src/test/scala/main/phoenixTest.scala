package main

import execute.Client
import execute.HodlCalulations.{hodlMintAmountFromERG, hodlPrice}
import org.ergoplatform.appkit.InputBox
import org.ergoplatform.appkit.impl.InputBoxImpl
import utils.{ContractCompile, explorerApi}

object phoenixTest extends App {
  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext
  val contractString = contracts.PhoenixContracts.phoenix_v1_hodlcoin_bank.contractScript
  val compilerObj = new ContractCompile(ctx)

  val dummyContractString = "sigmaProp(true)"
  val dummyContract = compilerObj.compileDummyContract(dummyContractString)

  val contract = compilerObj.compileBankContract(contractString, dummyContract)

  println(contract.getErgoTree.bytes.length)

}

object priceTest extends App {
  val inputBoxId = "785d02cb5bd1505c793585eed315ac923633fbf9164401d22fa8fd70dd7c4afc"
  val exp = new explorerApi()
  val box = new InputBoxImpl(exp.getErgoBoxfromID(inputBoxId))
    .asInstanceOf[InputBox]

  val txOperatorFee = 1000000L
  val minerFee = 1000000L

  val price = hodlPrice(box)
  val amntERGPaid = 1005968000L + minerFee
  val amntERGAfterFees = amntERGPaid - txOperatorFee - minerFee
  val hodlMintAmount = hodlMintAmountFromERG(box, amntERGAfterFees)

  println(hodlMintAmount)

  println(price)
}
