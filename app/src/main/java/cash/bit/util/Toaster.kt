package cash.bit.util

import android.content.Context
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import cash.bit.MainActivity

class Toaster {
    companion object {
        fun showToastMessage(context: Context, message: String) {
            showToastMessage(context, message, Toast.LENGTH_SHORT)
        }

        fun showToastMessage(context: Context, message: String, length: Int) {
            Toast.makeText(context, message, length).show()
        }

        fun showMessage(activity: MainActivity, message: String) {
            activity.runOnUiThread {
                activity.pay_button.isEnabled = true
                showToastMessage(activity, message)
            }
        }
    }
}