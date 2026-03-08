package com.blackbotek.app1

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> VelocidadFragment()
            1 -> FiltrosFragment()
            2 -> ResumenFragment()
            else -> VelocidadFragment()
        }
    }
}

