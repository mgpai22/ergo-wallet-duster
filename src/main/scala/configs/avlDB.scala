package configs

import com.google.gson.{GsonBuilder}

import scala.io.Source

case class AvlJson(
    manifestHex: String,
    digestHex: String,
    subTreeHex: Array[String]
)

class AVLJsonHelper(
    manifestHex: String,
    digestHex: String,
    subTreeHex: Array[String]
) {
  private val gson = new GsonBuilder()
    .setPrettyPrinting()
    .create()

  private val conf = AvlJson(
    manifestHex,
    digestHex,
    subTreeHex
  )

  def getJsonString: String = {
    return gson.toJson(conf)
  }

  def read(filePath: String): AvlJson = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[AvlJson])
  }

  def read: AvlJson = {
    gson.fromJson(gson.toJson(conf), classOf[AvlJson])
  }
}

object AVLJsonHelper {
  private val gson = new GsonBuilder()
    .setPrettyPrinting()
    .create()

  def read(filePath: String): AvlJson = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson
      .fromJson(jsonString, classOf[AvlJson])
  }

  def readJsonString(jsonString: String): AvlJson = {
    gson
      .fromJson(jsonString, classOf[AvlJson])
  }
}
