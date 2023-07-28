{

    // ===== Contract Information ===== //
    // Name: Phoenix HodlCoin Fee
    // Description: Contract guarding the fee box for the HodlCoin protocol.
    // Version: 1.0.0
    // Author: Luca D'Angelo (ldgaetano@protonmail.com), MGPai

    // ===== Box Contents ===== //
    // Tokens
    // None
    // Registers
    // None

    // ===== Relevant Transactions ===== //
    // 1. Fee Distribution Tx
    // Inputs: PhoenixFee1, ... , PhoenixFeeM
    // DataInputs: None
    // Outputs: Dev1PK, Dev2PK, Dev3PK, PhoenixPK
    // Context Variables: None

    // ===== Compile Time Constants ($) ===== //
    // $minerFee: Long

    // ===== Context Variables (@) ===== //
    // None

    val feeDenom = 100L
    val devPercentageNum: Long = 60L
    val phoenixPercentageNum: Long = 40L

    // ===== Relevant Variables ===== //
    val dev1Address: SigmaProp                  = PK("9exfustUCPDKXsfDrGNrmtkyLDwAie2rKKdUsPVa26RuBFaYeCL") // revert back to original address
    val dev2Address: SigmaProp                  = PK("9gnBtmSRBMaNTkLQUABoAqmU2wzn27hgqVvezAC9SU1VqFKZCp8")
    val dev3Address: SigmaProp                  = PK("9iE2MadGSrn1ivHmRZJWRxzHffuAk6bPmEv6uJmPHuadBY8td5u")
    val phoenixAddress: SigmaProp               = PK("9iPs1ujGj2eKXVg82aGyAtUtQZQWxFaki48KFixoaNmUAoTY6wV")
    val minerFeeErgoTreeBytesHash: Coll[Byte]   = fromBase16("e540cceffd3b8dd0f401193576cc413467039695969427df94454193dddfb375")

    // ===== Fee Distribution Tx ===== //
    val validFeeDistributionTx: Boolean = {

        // Outputs
        val dev1BoxOUT: Box     = OUTPUTS(0)
        val dev2BoxOUT: Box     = OUTPUTS(1)
        val dev3BoxOUT: Box     = OUTPUTS(2)
        val phoenixBoxOUT: Box  = OUTPUTS(3)
        val minerFeeBoxOUT: Box = OUTPUTS(4)

        val outputAmount: Long = OUTPUTS.map({ (output: Box) => output.value }).fold(0L, { (acc: Long, curr: Long) => acc + curr })
        val devAmount: Long = outputAmount - minerFeeBoxOUT.value // In case the miner fee increases in the future.

        val validPercentages: Boolean = {

            (devPercentageNum * feeDenom + phoenixPercentageNum * feeDenom) == (feeDenom * feeDenom) // (a/b + c/d = 1 => ad + cb = bd)

        }

        val validMinAmount: Boolean = {
            outputAmount >= 2000000L // this prevents dust transactions
        }

        val validDevBoxes: Boolean = {

            val devAllocation: Long = ((devPercentageNum * devAmount) / feeDenom) / 3L

            allOf(Coll(
                (dev1BoxOUT.value == devAllocation),
                (dev1BoxOUT.propositionBytes == dev1Address.propBytes),
                (dev2BoxOUT.value == devAllocation),
                (dev2BoxOUT.propositionBytes == dev2Address.propBytes),
                (dev3BoxOUT.value == devAllocation),
                (dev3BoxOUT.propositionBytes == dev3Address.propBytes)
            ))

        }

        val validPhoenixBox: Boolean = {

            allOf(Coll(
                (phoenixBoxOUT.value == (phoenixPercentageNum * devAmount) / feeDenom),
                (phoenixBoxOUT.propositionBytes == phoenixAddress.propBytes)
            ))

        }

        val validMinerFee: Boolean = {

            allOf(Coll(
                (minerFeeBoxOUT.value >= $minerFee), // In case the miner fee increases in the future
                (blake2b256(minerFeeBoxOUT.propositionBytes) == minerFeeErgoTreeBytesHash)
            ))

        }

        val validOutputSize: Boolean = (OUTPUTS.size == 5)

        allOf(Coll(
            validPercentages,
            validDevBoxes,
            validMinAmount,
            validPhoenixBox,
            validMinerFee,
            validOutputSize
        ))

    }

    sigmaProp(validFeeDistributionTx) && atLeast(1, Coll(dev1Address, dev2Address, dev3Address, phoenixAddress)) // Done so we are incentivized to not spam the miner fee.

}
