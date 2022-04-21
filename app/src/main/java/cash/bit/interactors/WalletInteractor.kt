package cash.bit.interactors

import org.bitcoinj.core.Address
import org.bitcoinj.kits.BIP47AppKit
import org.bitcoinj.kits.MultisigAppKit
import org.bitcoinj.wallet.Wallet
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import cash.bit.wallet.WalletManager

class WalletInteractor {
    fun getWalletKit(): BIP47AppKit? {
        return WalletManager.walletKit
    }

    fun getMultisigKit(): MultisigAppKit? {
        return WalletManager.multisigWalletKit
    }

    fun getBitcoinWallet(): Wallet? {
        return WalletManager.wallet
    }

    fun getSmartWallet(): Web3j? {
        return WalletManager.web3
    }

    fun getCredentials(): Credentials? {
        return WalletManager.credentials
    }

    fun getBitcoinAddress(): Address? {
        return getBitcoinWallet()?.currentReceiveAddress()?.toCash()
    }

    fun getFreshBitcoinAddress(): Address? {
        return getBitcoinWallet()?.freshReceiveAddress()?.toCash()
    }

    fun getSmartAddress(): String {
        return WalletManager.getSmartBchAddress().toString()
    }

    companion object {
        private var instance: WalletInteractor? = null
        fun getInstance(): WalletInteractor {
            if (instance == null) {
                instance = WalletInteractor()
            }
            return instance as WalletInteractor
        }
    }
}