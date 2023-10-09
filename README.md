# Wallet Duster

Dusts wallet in one transaction


# Setup
- Before running make sure to copy `serviceOwner.json.example` to `serviceOwner.json` and fill in the values
- Defaults are setup, only thing needed is mnemonic
- mnemonic password can be left blank, only needed if mnemonic is encrypted (most of the times it is not)
- addressIndex can be left to `0`, if you have multiple addresses in your wallet you can change this to the index of the address you want to use



# Run Using Scala

- install [scala](https://www.scala-lang.org/download/)
- install [sbt](https://www.scala-sbt.org/download.html)
- run `sbt "runMain app.Duster"` in the root directory of this project

# Run Using Jar
- install java
- download the [latest release](https://github.com/mgpai22/greasy-phoenix/releases/latest)
- run `java -jar duster-1.0.0.jar --conf <path to conf>`


- `--dryrun` flag can be used to test the transaction without submitting it to the network
  - For example:
    ```java
        java -jar duster-1.0.0.jar --conf <path to conf> --dryrun
    ```
    

# Build Jar
- make sure scala and sbt are installed
- run `sbt clean assembly` in the root directory of this project
- jar will be in root directory of this project

# Notes
- root directory means the directory where `README.md` file is located
