package execute
import configs.serviceOwnerConf
import node.{MainNetNodeExplorerInfo, NodeInfo, TestNetNodeExplorerInfo}
import org.ergoplatform.appkit.NetworkType

case class DefaultNodeInfo(
    override val nodeUrl: String,
    apiUrl: String,
    networkType: NetworkType
) extends NodeInfo(
      mainNetNodeExplorerInfo = MainNetNodeExplorerInfo(
        mainnetNodeUrl = nodeUrl,
        mainnetExplorerUrl = apiUrl
      ),
      testNetNodeExplorerInfo = TestNetNodeExplorerInfo(
        testnetNodeUrl = nodeUrl,
        testnetExplorerUrl = apiUrl
      ),
      networkType = networkType
    )
