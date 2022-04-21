package cash.bit.interactors

import org.bitcoinj.core.CashAddressFactory
import org.bitcoinj.core.Coin
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.SendRequest
import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.utils.Numeric
import cash.bit.util.Constants
import cash.bit.wallet.WalletManager
import java.math.BigInteger

class TransactionInteractor {
    private val walletInteractor = WalletInteractor.getInstance()

    fun createHopToSmartBch(sendMax: Boolean, amount: Coin): SendRequest {
        val incomingAddress = CashAddressFactory.create()
            .getFromFormattedAddress(WalletManager.parameters, Constants.HOPCASH_BCH_INCOMING)
        val opReturnData =
            ScriptBuilder.createOpReturnScript(walletInteractor.getSmartAddress().toByteArray())
        val tempTx = org.bitcoinj.core.Transaction(WalletManager.parameters)
        return if (sendMax) {
            tempTx.addOutput(Coin.ZERO, opReturnData)
            tempTx.addOutput(Coin.ZERO, incomingAddress)
            val tempReq = SendRequest.forTx(tempTx)
            tempReq.shuffleOutputs = false
            tempReq.emptyWalletOutput = 1
            tempReq.emptyWallet = true
            tempReq
        } else {
            tempTx.addOutput(Coin.ZERO, opReturnData)
            tempTx.addOutput(amount, incomingAddress)
            val tempReq = SendRequest.forTx(tempTx)
            tempReq.shuffleOutputs = false
            tempReq
        }
    }

    fun createHopToBitcoin(
        sendMax: Boolean,
        nonce: BigInteger,
        gasPrice: BigInteger,
        amount: BigInteger
    ): RawTransaction? {
        val ourAddress = walletInteractor.getSmartAddress()
        val incomingAddress = Constants.HOPCASH_SBCH_INCOMING
        val dataField = Numeric.prependHexPrefix(
            Hex.toHexString(
                walletInteractor.getBitcoinAddress()?.toCash().toString().toByteArray()
            )
        )
        return if (sendMax) {
            val tempReq = Transaction.createFunctionCallTransaction(
                ourAddress,
                nonce,
                gasPrice,
                BigInteger.valueOf(22000),
                incomingAddress,
                amount,
                dataField
            )
            val gasEstimate =
                walletInteractor.getSmartWallet()?.ethEstimateGas(tempReq)?.send()?.amountUsed
            val gasValue = gasEstimate?.multiply(gasPrice) ?: return null
            val sendValue = amount.subtract(gasValue)
            val req = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                BigInteger.valueOf(22000),
                incomingAddress,
                sendValue,
                dataField
            )
            req
        } else {
            val req = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                BigInteger.valueOf(22000),
                incomingAddress,
                amount,
                dataField
            )
            req
        }
    }

    companion object {
        private var instance: TransactionInteractor? = null
        fun getInstance(): TransactionInteractor {
            if (instance == null) {
                instance = TransactionInteractor()
            }
            return instance as TransactionInteractor
        }
    }
}