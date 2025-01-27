package cash.bit.ui.main.fragment.send

import android.app.Activity
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_send_home.view.*
import cash.bit.R
import cash.bit.qr.QRHelper
import cash.bit.ui.main.MainFragmentDirections
import cash.bit.util.Constants
import cash.bit.util.PayloadHelper
import cash.bit.util.PaymentType
import cash.bit.util.UriHelper
import cash.bit.wallet.WalletManager

/**
 * A placeholder fragment containing a simple view.
 */
class SendHomeFragment : Fragment() {
    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (Constants.ACTION_HOP_TO_BCH == intent.action) {
                    findNavController().navigate(MainFragmentDirections.navToSend(Constants.HOPCASH_SBCH_INCOMING))
                } else if (Constants.ACTION_HOP_TO_SBCH == intent.action) {
                    findNavController().navigate(MainFragmentDirections.navToSend(Constants.HOPCASH_BCH_INCOMING))
                }
            } catch (e: Exception) {

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_send_home, container, false)

        root.scan_qr_code_button.setOnClickListener {
            QRHelper().startQRScan(this, Constants.REQUEST_CODE_SCAN_QR)
        }

        root.paste_address_button.setOnClickListener {
            val clipBoard =
                requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val pasteData = clipBoard.primaryClip?.getItemAt(0)?.text.toString()
            if (isValidPaymentType(pasteData) || PayloadHelper.isMultisigPayload(pasteData)) {
                findNavController().navigate(
                    MainFragmentDirections.navToSend(
                        pasteData
                    )
                )
            }
        }

        root.donate_button.setOnClickListener {
            findNavController().navigate(
                MainFragmentDirections.navToSend(
                    Constants.DONATION_ADDRESS
                )
            )
        }

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_HOP_TO_BCH)
        filter.addAction(Constants.ACTION_HOP_TO_SBCH)
        activity?.let { LocalBroadcastManager.getInstance(it).registerReceiver(receiver, filter) }

        return root
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