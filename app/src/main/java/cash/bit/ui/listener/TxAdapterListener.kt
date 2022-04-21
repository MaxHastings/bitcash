package cash.bit.ui.listener

import org.bitcoinj.core.Transaction

interface TxAdapterListener {
    fun onClickTransaction(tx: Transaction)
}