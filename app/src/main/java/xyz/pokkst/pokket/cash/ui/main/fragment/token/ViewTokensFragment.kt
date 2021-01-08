package xyz.pokkst.pokket.cash.ui.main.fragment.token

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bitcoinj.core.slp.SlpTokenBalance
import xyz.pokkst.pokket.cash.MainActivity
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.ui.NonScrollListView
import xyz.pokkst.pokket.cash.ui.SlpTokenListEntryView
import xyz.pokkst.pokket.cash.util.Constants
import xyz.pokkst.pokket.cash.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class ViewTokensFragment : Fragment() {
    var root: View? = null
    private var srlSLP: SwipeRefreshLayout? = null
    private var slpList: NonScrollListView? = null
    private var recalculationJob: Job? = null

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_MAIN_ENABLE_PAGER == intent.action) {
                this@ViewTokensFragment.findNavController()
                    .popBackStack(R.id.sendHomeFragment, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as? MainActivity)?.toggleSendScreen(false)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_view_tokens, container, false)
        (activity as? MainActivity)?.enableTokensScreen()
        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_MAIN_ENABLE_PAGER)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
        srlSLP = root?.findViewById(R.id.srlSLP)
        slpList = root?.findViewById(R.id.slpList)
        this.srlSLP?.setOnRefreshListener { this.refresh() }
        this.slpList?.setOnItemClickListener { parent, view, position, id ->
            val tokenBalance = WalletManager.walletKit?.slpBalances?.get(position)
            val tokenId = tokenBalance?.tokenId
            findNavController().navigate(
                ViewTokensFragmentDirections.navToSendFromViewTokens(
                    null,
                    tokenId
                )
            )
        }

        refresh()
        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        recalculationJob?.cancel()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun refresh() {
        recalculationJob?.cancel()
        recalculationJob = lifecycleScope.launch(Dispatchers.IO) {
            WalletManager.walletKit?.recalculateSlpUtxos()

            activity?.runOnUiThread {
                setSLPList()
            }
        }
        val srlSlp = srlSLP
        if (srlSlp != null && srlSlp.isRefreshing) srlSLP?.isRefreshing = false
    }

    private fun setSLPList() {
        val items = WalletManager.walletKit?.slpBalances?.toList() ?: listOf()
        val itemsAdapter = object : ArrayAdapter<SlpTokenBalance>(
            requireContext(),
            R.layout.token_list_cell,
            items
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return SlpTokenListEntryView.instanceOf(
                    activity,
                    position,
                    R.layout.token_list_cell
                )
            }
        }

        slpList?.adapter = itemsAdapter
        slpList?.refreshDrawableState()

        if (items.isEmpty()) {
            root?.findViewById<TextView>(R.id.loading_tokens_view)?.visibility = View.GONE
            root?.findViewById<TextView>(R.id.no_tokens_view)?.visibility = View.VISIBLE
            slpList?.visibility = View.GONE
        } else {
            root?.findViewById<TextView>(R.id.loading_tokens_view)?.visibility = View.GONE
            root?.findViewById<TextView>(R.id.no_tokens_view)?.visibility = View.GONE
            slpList?.visibility = View.VISIBLE
        }
    }
}