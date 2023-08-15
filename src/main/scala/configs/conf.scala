package configs

import com.google.gson.GsonBuilder

import scala.io.Source

case class ServiceOwnerConfig(
    txOperatorMnemonic: String,
    txOperatorMnemonicPw: String,
    nodeUrl: String,
    apiUrl: String,
    addressIndex: Int,
    hodlErgTokenId: String,
    bankBoxSingletonTokenId: String
)

object serviceOwnerConf {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): ServiceOwnerConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[ServiceOwnerConfig])
  }
}
