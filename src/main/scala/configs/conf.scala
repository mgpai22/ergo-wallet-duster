package configs

import com.google.gson.{Gson, GsonBuilder, JsonElement}

import java.io.{FileWriter, Writer}
import scala.io.Source

case class ContractsConfig(
    Contracts: Config
)

case class Config(
    bankContract: BankContract,
    proxyContract: ProxyContract,
    feeContract: FeeContract
)
case class BankContract(
    contract: String,
    bankSingleton: String,
    hodlCoin: String
)
case class FeeContract(
    contract: String
)
case class ProxyContract(
    contract: String
)

class conf(
    bankContract: String,
    bankSingleton: String,
    hodlCoin: String,
    feeContract: String,
    proxyContract: String
) {
  val bankContractInstance: BankContract =
    BankContract(bankContract, bankSingleton, hodlCoin)
  val feeContractInstance: FeeContract =
    FeeContract(feeContract)
  val proxyContractInstance: ProxyContract = ProxyContract(proxyContract)

  val conf = Config(
    bankContractInstance,
    proxyContractInstance,
    feeContractInstance
  )
  val newConfig: ContractsConfig = ContractsConfig(conf)
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def write(filePath: String): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(this.gson.toJson(this.newConfig))
    writer.close()
  }

  def read(filePath: String): ContractsConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[ContractsConfig])
  }

}

object conf {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): ContractsConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[ContractsConfig])
  }

  def write(filePath: String, newConfig: ContractsConfig): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(this.gson.toJson(newConfig))
    writer.close()
  }

}

case class ServiceOwnerConfig(
    txOperatorMnemonic: String,
    txOperatorMnemonicPw: String,
    nodeUrl: String,
    apiUrl: String,
    minTxOperatorFee: Long,
    minBoxValue: Long,
    minMinerFee: Long
)

object serviceOwnerConf {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): ServiceOwnerConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[ServiceOwnerConfig])
  }
}
