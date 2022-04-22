package cash.bit.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cash.bit.R
import cash.bit.interactors.BalanceInteractor
import cash.bit.qr.QRHelper
import cash.bit.util.*
import cash.bit.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class MainFragment : Fragment() {

    val balanceInteractor = BalanceInteractor.getInstance()

    var fiatBalanceView : TextView? = null

    var bchBalanceView : TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_main, container, false)

        fiatBalanceView = root.findViewById<TextView>(R.id.fiat_balance);
        bchBalanceView = root.findViewById<TextView>(R.id.bch_balance);

        root.findViewById<Button>(R.id.send_button).setOnClickListener {
            QRHelper().startQRScan(this, Constants.REQUEST_CODE_SCAN_QR)
        }
        root.findViewById<Button>(R.id.receive_button).setOnClickListener {
            findNavController().navigate(R.id.receiveAmountFragment)
        }

        WalletManager.refreshEvents.observe(viewLifecycleOwner, { event ->
            if (event != null) {
                refresh()
            }
        })

        WalletManager.syncPercentage.observe(viewLifecycleOwner, { pct ->
            refresh()
        })


        refresh()
        return root
    }

    fun refresh() {
        try {
            val bch = balanceInteractor.getBitcoinBalance()
            val bchStr = BalanceFormatter.formatBalance(bch.toDouble(), "#.########")
            val fiat = bch.toDouble() * PriceHelper.price
            val fiatStr = BalanceFormatter.formatBalance(fiat, "0.00")
            fiatBalanceView?.text = "$${fiatStr}"
            bchBalanceView?.text = String.format("%.8f BCH", bch)
        } catch (e: Exception) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.REQUEST_CODE_SCAN_QR) {
                if (data != null) {
                    val scanData = data.getStringExtra(Constants.QR_SCAN_RESULT)
                    if (scanData != null) {
                        if (isValidPaymentType(scanData) || PayloadHelper.isMultisigPayload(scanData)) {
                            findNavController().navigate(
                                MainFragmentDirections.navToSend(
                                    scanData
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isValidPaymentType(address: String): Boolean {
        return if (WalletManager.isMultisigKit) {
            UriHelper.parse(address)?.paymentType == PaymentType.ADDRESS
        } else {
            UriHelper.parse(address) != null
        }
    }
}