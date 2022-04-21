package cash.bit.ui.main.fragment.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.common.base.Splitter
import kotlinx.android.synthetic.main.fragment_intro_bg.view.*
import kotlinx.android.synthetic.main.fragment_restore_wallet.view.*
import org.bitcoinj.crypto.MnemonicCode
import cash.bit.MainActivity
import cash.bit.R
import cash.bit.util.*

/**
 * A placeholder fragment containing a simple view.
 */
class RestoreWalletFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_restore_wallet, container, false)

        root.continue_button.setOnClickListener {
            val seedStr = root.recover_wallet_edit_text.text.toString().trim()
            if (isMnemonicValid(seedStr)) {
                val isMultisigChecked = root.multsig_checkbox.isChecked
                if (isMultisigChecked) {
                    val action = RestoreWalletFragmentDirections.navToMyFollowingKey(seedStr, true)
                    findNavController().navigate(action)
                    StatusBarHelper.setStatusBarColor(requireActivity(), R.color.extra_light_grey)
                } else {
                    val bip39passphrase = root.bip39_passphrase_edit_text.text.toString().trim()
                    val customPath = root.custom_path_edittext.text.toString().trim()
                    val path = DerivationParser.parse(customPath)
                    StatusBarHelper.setStatusBarColor(
                        requireActivity(),
                        R.color.extra_light_grey
                    )
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(Constants.EXTRA_SEED, seedStr)
                    intent.putExtra(Constants.EXTRA_NEW, false)
                    if (bip39passphrase.isNotEmpty()) {
                        intent.putExtra(Constants.EXTRA_PASSPHRASE, bip39passphrase)
                    }

                    if (path != null && customPath.isNotEmpty() && root.custom_path_checkbox.isChecked) {
                        PrefsHelper.instance(context)?.edit()?.putString(Constants.PREF_DERIVATION_PATH, customPath)?.apply()
                        intent.putExtra(Constants.EXTRA_DERIVATION, customPath)
                    }

                    if((customPath.isEmpty() && root.custom_path_checkbox.isChecked) || (path == null && customPath.isNotEmpty() && root.custom_path_checkbox.isChecked)) {
                        context?.let { it1 -> Toaster.showToastMessage(it1, "invalid derivation path") }
                        return@setOnClickListener
                    }

                    startActivity(intent)
                }
            }
        }

        root.multsig_checkbox.setOnCheckedChangeListener { _, isChecked ->
            root.custom_path_checkbox.isChecked = if(isChecked) false else root.custom_path_checkbox.isChecked
            root.bip39_passphrase_edit_text.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        root.custom_path_checkbox.setOnCheckedChangeListener { _, isChecked ->
            root.multsig_checkbox.isChecked = if(isChecked) false else root.multsig_checkbox.isChecked
            root.custom_path_edittext.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        root.intro_left_button.setOnClickListener {
            findNavController().popBackStack()
        }
        return root
    }

    private fun isMnemonicValid(seedStr: String): Boolean {
        val seedList = Splitter.on(' ').splitToList(seedStr)
        return try {
            MnemonicCode().check(seedList)
            true
        } catch (e: Exception) {
            false
        }
    }
}