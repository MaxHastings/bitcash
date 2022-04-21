package cash.bit.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_epk.view.*
import cash.bit.R
import cash.bit.util.ClipboardHelper
import cash.bit.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsEpkFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_epk, container, false)
        val xpub = WalletManager.wallet?.watchingKey?.serializePubB58(WalletManager.parameters)
        root.the_epk.text = xpub
        root.the_epk.setOnClickListener {
            ClipboardHelper.copyToClipboard(activity, xpub)
        }
        return root
    }
}