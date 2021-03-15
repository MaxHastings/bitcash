package xyz.pokkst.pokket.cash.ui.main.fragment.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.transaction_item_expanded_received.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.slp.SlpTransaction
import org.bitcoinj.script.ScriptPattern
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.BalanceFormatter
import xyz.pokkst.pokket.cash.util.ClipboardHelper
import xyz.pokkst.pokket.cash.util.PriceHelper
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.util.*


/**
 * A placeholder fragment containing a simple view.
 */
class TransactionReceivedFragment : Fragment() {
    var isSlp: Boolean = false
    var slpTransaction: SlpTransaction? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.transaction_item_expanded_received, container, false)
        val txid = arguments?.getString("txid", "")
        val tx = WalletManager.wallet?.getTransaction(Sha256Hash.wrap(txid))
        val args = arguments
        if (args != null)
            isSlp = args.getBoolean("slp", false)

        if (isSlp) {
            slpTransaction = SlpTransaction(tx)
        }
        root.tx_id.setOnClickListener {
            ClipboardHelper.copyToClipboard(activity, txid)
        }
        root.tx_hash_text.text = txid

        root.tx_status_text.text = if (tx?.confidence?.depthInBlocks!! > 0) {
            "confirmed in block #${tx.confidence.appearedAtChainHeight}"
        } else {
            "verified, waiting for confirmation"
        }

        val fromAddresses = ArrayList<String>()
        for (x in tx.inputs.indices) {
            fromAddresses.add(tx.inputs[x].outpoint.toString())
        }
        setReceivedFromAddresses(root.general_tx_from_layout, fromAddresses)

        val toAddresses = ArrayList<String?>()
        val toAmounts = ArrayList<Long>()
        val slpTx = slpTransaction
        val slpToken = WalletManager.walletKit?.getSlpToken(slpTx?.tokenId) ?: WalletManager.walletKit?.getNft(slpTx?.tokenId)
        for (x in tx.outputs.indices) {
            val slpUtxo = if (slpTx != null) {
                try {
                    slpTx.slpUtxos[x - 1]
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            if (ScriptPattern.isOpReturn(tx.outputs[x].scriptPubKey)) {
                toAddresses.add("OP_RETURN")
            } else {
                val address =
                    if (isSlp && (slpUtxo != null || x == slpTx?.slpOpReturn?.mintingBatonVout) && slpToken != null) {
                        tx.outputs[x].scriptPubKey.getToAddress(WalletManager.parameters).toSlp()
                            .toString()
                    } else {
                        tx.outputs[x].scriptPubKey.getToAddress(WalletManager.parameters).toCash()
                            .toString()
                    }
                toAddresses.add(address)
            }

            toAmounts.add(tx.outputs[x].value.value)
        }

        val bchReceived = if (slpTx != null && slpToken != null) {
            slpTx.getRawValue(WalletManager.wallet).scaleByPowerOfTen(-slpToken.decimals).toDouble()
        } else {
            tx.getValueSentToMe(WalletManager.wallet).toPlainString().toDouble()
        }
        root.tx_amount_text.text = if (slpTx != null && slpToken != null) {
            "${BalanceFormatter.formatBalance(bchReceived, "#.#########")} ${slpToken.ticker}"

        } else {
            resources.getString(
                R.string.tx_amount_moved,
                BalanceFormatter.formatBalance(bchReceived, "#.########")
            )
        }

        if (!isSlp || slpToken == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val fiatValue = bchReceived * PriceHelper.price
                activity?.runOnUiThread {
                    root.tx_exchange_text.text =
                        "($${BalanceFormatter.formatBalance(fiatValue, "0.00")})"
                }
            }
        }

        setReceivedToAddresses(root.general_tx_to_layout, toAddresses, toAmounts)

        return root
    }

    private fun setReceivedFromAddresses(
        view: LinearLayout,
        addresses: ArrayList<String>
    ) {
        val inflater = requireActivity().layoutInflater
        for (address in addresses) {
            val addressBlock =
                inflater.inflate(R.layout.transaction_received_from_addresses, null) as LinearLayout
            val txFrom =
                addressBlock.findViewById<View>(R.id.tx_from_text) as TextView
            val txFromDescription =
                addressBlock.findViewById<View>(R.id.tx_from_description) as TextView
            if (address != null && address.isNotEmpty()) {
                txFrom.text = address
                txFromDescription.text = getString(R.string.utxo)
                view.addView(addressBlock)
            }
        }
    }

    private fun setReceivedToAddresses(
        view: LinearLayout,
        addresses: ArrayList<String?>,
        amounts: ArrayList<Long>
    ) {
        val inflater = requireActivity().layoutInflater
        val slpTx = slpTransaction
        val slpToken = WalletManager.walletKit?.getSlpToken(slpTx?.tokenId) ?: WalletManager.walletKit?.getNft(slpTx?.tokenId)
        val txid = arguments?.getString("txid", "")
        val tx = WalletManager.wallet?.getTransaction(Sha256Hash.wrap(txid))
        for (i in addresses.indices) {
            val utxoIsMine = if (tx != null) {
                tx.outputs[i].isMine(WalletManager.wallet)
            } else {
                false
            }

            val slpUtxo = if (slpTx != null) {
                try {
                    slpTx.slpUtxos[i - 1]
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            val addressBlock =
                inflater.inflate(R.layout.transaction_received_to_addresses, null) as RelativeLayout
            val txTo =
                addressBlock.findViewById<View>(R.id.tx_to_text) as TextView
            val txToDescription =
                addressBlock.findViewById<View>(R.id.tx_to_description) as TextView
            val txToAmount =
                addressBlock.findViewById<View>(R.id.tx_to_amount_text) as TextView
            val txToExchange =
                addressBlock.findViewById<View>(R.id.tx_to_exchange_text) as TextView
            //BRAnimator.showCopyBubble(activity, addressBlock, txTo)
            if (addresses[i] != null && !addresses[i]!!.isEmpty()) {
                txTo.text = addresses[i]
                if (addresses[i] == "OP_RETURN") {
                    txToDescription.text = getString(R.string.op_return_address)
                } else {
                    txToDescription.text = getString(R.string.wallet_address)
                }
                val amountInBch = if (slpUtxo != null && slpToken != null) {
                    slpUtxo.tokenAmountRaw.toBigDecimal().scaleByPowerOfTen(-slpToken.decimals)
                        .toDouble()
                } else {
                    amounts[i] / 100000000.0
                }
                txToAmount.text = if (slpUtxo != null && slpToken != null) {
                    "${BalanceFormatter.formatBalance(
                        amountInBch,
                        "#.#########"
                    )} ${slpToken.ticker}"
                } else {
                    if (utxoIsMine) {
                        if (i == slpTx?.slpOpReturn?.mintingBatonVout && isSlp && slpTx.slpOpReturn?.hasMintingBaton()!! && slpToken != null) {
                            "Minting Baton"
                        } else {
                            resources.getString(
                                R.string.tx_amount_moved,
                                "${BalanceFormatter.formatBalance(amountInBch, "#.########")}"
                            )
                        }
                    } else {
                        if (i == slpTx?.slpOpReturn?.mintingBatonVout && isSlp && slpTx.slpOpReturn?.hasMintingBaton()!! && slpToken != null) {
                            "Minting Baton"
                        } else {
                            resources.getString(
                                R.string.tx_amount_moved,
                                "${BalanceFormatter.formatBalance(amountInBch, "#.########")}"
                            )
                        }
                    }
                }
                txToExchange.text = if (slpUtxo != null && slpToken != null) {
                    null
                } else {
                    if (i == slpTx?.slpOpReturn?.mintingBatonVout && slpTx.slpOpReturn?.hasMintingBaton()!! && slpToken != null) {
                        null
                    } else {
                        val amountInFiat = amountInBch * PriceHelper.price
                        "($${BalanceFormatter.formatBalance(amountInFiat, "0.00")})"
                    }
                }

                if (!utxoIsMine) {
                    txToAmount.setTextColor(resources.getColor(R.color.black))
                    txToExchange.setTextColor(resources.getColor(R.color.black))
                }

                view.addView(addressBlock)
            }
        }
    }
}