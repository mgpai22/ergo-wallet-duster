package main

import contracts.PhoenixContracts
import execute.Client
import utils.ContractCompile

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
