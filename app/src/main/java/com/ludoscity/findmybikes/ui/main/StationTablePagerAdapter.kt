package com.ludoscity.findmybikes.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import com.ludoscity.findmybikes.ui.table.StationTableFragment
import com.ludoscity.findmybikes.ui.table.TableFragmentModelFactory
import com.ludoscity.findmybikes.ui.table.TableFragmentViewModel
import java.text.NumberFormat

/**
 * Created by F8Full on 2015-10-19.
 * Adapter for view pager displaying station lists
 */
class StationTablePagerAdapter(
        fm: FragmentManager,
        private val bikeTableFragmentModelFactory: TableFragmentModelFactory,
        private val dockTableFragmentModelFactory: TableFragmentModelFactory) : SmartFragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        val toReturn = StationTableFragment()

        val args = Bundle()
        if (position == BIKE_STATIONS) {
            args.putBoolean("isDockTable", bikeTableFragmentModelFactory.isDockTable)
        } else {
            args.putBoolean("isDockTable", dockTableFragmentModelFactory.isDockTable)
        }

        args.putSerializable("numFormat", NumberFormat.getInstance())

        toReturn.arguments = args

        return toReturn
    }

    override fun getCount(): Int {
        return NUM_ITEMS
    }

    private fun retrieveListFragment(position: Int): StationTableFragment? {
        return getRegisteredFragment(position) as? StationTableFragment
    }

    fun smoothScrollHighlightedInViewForTable(_tableId: Int) {
        retrieveListFragment(BIKE_STATIONS)?.let { bikeFrag ->
            var tableModel =
                    ViewModelProviders.of(bikeFrag, bikeTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
            if (_tableId == DOCK_STATIONS)
                retrieveListFragment(DOCK_STATIONS)?.let { dockFrag ->
                    tableModel = ViewModelProviders.of(dockFrag, dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
                }

            tableModel.smoothScrollSelectionInView()
        }
    }

    companion object {

        var BIKE_STATIONS = 0
        var DOCK_STATIONS = 1

        private const val NUM_ITEMS = 2


    }
}
