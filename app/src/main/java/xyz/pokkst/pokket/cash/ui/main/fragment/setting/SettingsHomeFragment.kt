package xyz.pokkst.pokket.cash.ui.main.fragment.setting

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_settings_home.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.slp.SlpOpReturn
import org.bitcoinj.core.slp.SlpTransaction
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.SettingsActivity
import xyz.pokkst.pokket.cash.ui.TransactionListEntryView
import xyz.pokkst.pokket.cash.util.BalanceFormatter
import xyz.pokkst.pokket.cash.util.PriceHelper
import xyz.pokkst.pokket.cash.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsHomeFragment : Fragment() {
    private var sentColor = 0
    private var receivedColor = 0
    private var txList = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_home, container, false)
        sentColor = Color.parseColor("#FF5454")
        receivedColor = Color.parseColor("#00BF00")
        this.setArrayAdapter(root, WalletManager.wallet)

        root.about.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            navigate(R.id.nav_to_about)
        }
        root.about.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.about)

        root.recovery_phrase.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            navigate(R.id.nav_to_phrase)
        }
        root.recovery_phrase.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.recovery_phrase_label)

        root.custom_node.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            navigate(R.id.nav_to_node)
        }
        root.custom_node.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.node_label)

        root.extended_public_key.findViewById<RelativeLayout>(R.id.setting_layout)
            .setOnClickListener {
                navigate(R.id.nav_to_epk)
            }
        root.extended_public_key.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.epk_label)

        root.shift_service.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shift.pokket.cash/a/mmG1iwJRO"))
            startActivity(browserIntent)
        }
        root.shift_service.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.shift_service_label)

        root.start_recovery_wallet.setOnClickListener {
            navigate(R.id.nav_to_wipe)
        }

        setSyncStatus(root)

        root.transactions_list.setOnItemClickListener { parent, view, position, id ->
            (activity as? SettingsActivity)?.adjustDeepMenu(1)
            val txid = txList[position]
            val tx = WalletManager.wallet?.getTransaction(Sha256Hash.wrap(txid))
            val amount = tx?.getValue(WalletManager.wallet)
            val isSlp = SlpOpReturn.isSlpTx(tx)
            val slpAmount = if (isSlp) {
                val slpTx = SlpTransaction(tx)
                val slpToken = WalletManager.walletKit?.getSlpToken(slpTx.tokenId)
                if (slpToken != null) {
                    val slpAmount = slpTx.getRawValue(WalletManager.wallet)
                        .scaleByPowerOfTen(-slpToken.decimals).toDouble()
                    slpAmount
                } else {
                    0.0
                }
            } else {
                0.0
            }
            if (amount?.isPositive == true || slpAmount > 0) {
                findNavController().navigate(
                    SettingsHomeFragmentDirections.navToTxReceived(
                        txid,
                        isSlp
                    )
                )
            } else if (amount?.isNegative == true) {
                findNavController().navigate(
                    SettingsHomeFragmentDirections.navToTxSent(
                        txid,
                        isSlp
                    )
                )
            }
        }

        root.more_transactions.setOnClickListener {
            navigate(R.id.nav_to_tx_list)
        }

        return root
    }

    private fun setSyncStatus(root: View?) {
        val lastSeenBlockHeight = WalletManager.wallet?.lastBlockSeenHeight
        val bestBlockHeight = WalletManager.kit?.peerGroup()?.mostCommonChainHeight
        when {
            bestBlockHeight == 0 ->
                root?.sync_status?.text = resources.getString(R.string.not_syncing)
            bestBlockHeight != lastSeenBlockHeight ->
                root?.sync_status?.text = resources.getString(R.string.syncing)
            bestBlockHeight == lastSeenBlockHeight ->
                root?.sync_status?.text = resources.getString(R.string.synced)
        }
    }

    private fun navigate(navResId: Int) {
        (activity as? SettingsActivity)?.adjustDeepMenu(1)
        findNavController().navigate(navResId)
    }

    private fun setArrayAdapter(root: View, wallet: Wallet?) {
        setListViewShit(root, wallet)
    }

    private fun setListViewShit(root: View, wallet: Wallet?) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (wallet != null) {
                val txListFromWallet = wallet.getRecentTransactions(0, false)
                txList = ArrayList<String>()

                if (txListFromWallet.isNotEmpty()) {
                    val txListFormatted = ArrayList<Map<String, String>>()

                    requireActivity().runOnUiThread {
                        if (txListFromWallet.size > 5) {
                            root.more_transactions.visibility = View.VISIBLE
                        }
                        root.no_transactions.visibility = View.GONE
                    }

                    for (x in 0 until 5) {
                        val tx = try {
                            txListFromWallet[x]
                        } catch (e: Exception) {
                            continue
                        }
                        val isSlp = SlpOpReturn.isSlpTx(tx)
                        val confirmations = tx.confidence.depthInBlocks
                        val value = tx.getValue(wallet)
                        val timestamp = tx.updateTime.time.toString()
                        val datum = HashMap<String, String>()
                        var ticker = ""
                        val amountStr = if (isSlp) {
                            val slpTx = SlpTransaction(tx)
                            val slpToken =
                                WalletManager.walletKit?.getSlpToken(slpTx.tokenId)
                            if (slpToken != null) {
                                ticker = slpToken.ticker
                                val slpAmount = slpTx.getRawValue(WalletManager.wallet)
                                    .scaleByPowerOfTen(-slpToken.decimals).toDouble()
                                BalanceFormatter.formatBalance(slpAmount, "#.#########")
                            } else {
                                value.toPlainString()
                            }
                        } else {
                            value.toPlainString()
                        }

                        datum["action"] =
                            if (value.isPositive || amountStr.toDouble() > 0) {
                                "received"
                            } else {
                                "sent"
                            }

                        datum["ticker"] = ticker
                        datum["slp"] = if (isSlp) "true" else "false"
                        datum["amount"] = amountStr
                        datum["fiatAmount"] = BalanceFormatter.formatBalance(
                            (amountStr.toDouble() * PriceHelper.price),
                            "0.00"
                        )
                        datum["timestamp"] = timestamp

                        txList.add(tx.txId.toString())
                        txListFormatted.add(datum)
                    }

                    val itemsAdapter = object : SimpleAdapter(
                        requireContext(),
                        txListFormatted,
                        R.layout.transaction_list_item,
                        null,
                        null
                    ) {
                        override fun getView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            return TransactionListEntryView.instanceOf(activity, position, txListFormatted)
                        }
                    }
                    activity?.runOnUiThread {
                        root.transactions_list.adapter = itemsAdapter
                    }
                } else {
                    activity?.runOnUiThread {
                        root.space.visibility = View.GONE
                        root.transactions_list.visibility = View.GONE
                        root.no_transactions.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}