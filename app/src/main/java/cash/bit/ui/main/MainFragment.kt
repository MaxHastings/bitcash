package cash.bit.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import cash.bit.R
import cash.bit.ui.ToggleViewPager


/**
 * A placeholder fragment containing a simple view.
 */
class MainFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)

        val viewPager = root.findViewById<ToggleViewPager>(R.id.view_pager)
        val tabs = root.findViewById<TabLayout>(R.id.tabs)
        val sectionsPagerAdapter = context?.let { SectionsPagerAdapter(it, childFragmentManager) }
        viewPager.adapter = sectionsPagerAdapter
        tabs.setupWithViewPager(viewPager)
        return root
    }
}