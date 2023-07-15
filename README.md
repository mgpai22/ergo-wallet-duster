# Ergo Off-Chain Bot Template

This is a template that can be cloned to implement your off-chain ergo transaction logic!


# Included Packages
- Ergo Appkit
- GetBlok Plasma
- Exle Edge
- Akka
- Apache Http
- Gson Json Serialization

# Usage
- Add contracts in src/main/scala/resources
- Add boxes in src/main/scala/utils/Outboxes or /InputBoxes
- Add api calls in src/main/scala/utils/explorerApi
- Build complete transactions in src/main/scala/execute/TxBuildUtility
- Write code which will be driven by akka in src/main/scala/execute/akkaFunctions
- Start and configure akka in src/main/scala/app/Main
- Run modules/test code in src/test/scala/main
- Add node and mnemonic information in serviceOwner.json
- If you want to add more items in the serviceOwner.json make sure to add to the ServiceOwnerConfig case class in  src/main/scala/configs/conf
