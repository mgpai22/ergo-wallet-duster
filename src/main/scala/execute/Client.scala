package execute
import configs.serviceOwnerConf
import node.BaseClient
import utils.network

class Client(
    nodeUrl: String = serviceOwnerConf.read("serviceOwner.json").nodeUrl,
    apiUrl: String = serviceOwnerConf.read("serviceOwner.json").apiUrl
) extends BaseClient(
      nodeInfo = execute.DefaultNodeInfo(
        nodeUrl,
        apiUrl,
        new network(nodeUrl).getNetworkType
      )
    ) {}
