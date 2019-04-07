package com.ludoscity.findmybikes.ui.table

import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.ludoscity.findmybikes.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Adapter used to show the datas of every stationItem
 */
class StationTableRecyclerViewAdapter(private val tableFragmentModel: TableFragmentViewModel) : RecyclerView.Adapter<StationTableRecyclerViewAdapter.BikeStationListItemViewHolder>() {

    /**
     * Created by F8Full on 2015-03-18.
     *
     * DiffCallback class to compare new items dataset and turn differences into instructions for the recyclerview
     * see : https://developer.android.com/reference/android/support/v7/util/DiffUtil
     */
    private inner class TableDiffCallback(private val mOldList: List<StationTableItemData>, private val mNewList: List<StationTableItemData>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return mOldList.size
        }

        override fun getNewListSize(): Int {
            return mNewList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return mNewList[newItemPosition].locationHash == mOldList[oldItemPosition].locationHash
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return mNewList[newItemPosition] == mOldList[oldItemPosition]
        }
    }

    private var items: List<StationTableItemData> = emptyList()

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)
    private val coroutineScopeMAIN = CoroutineScope(Dispatchers.Main)

    fun loadItems(newItems: List<StationTableItemData>) {

        //val randomDebugloadID = Math.random()

        //This is slow but safe...
        /*val diffResult = DiffUtil.calculateDiff(TableDiffCallback(items, newItems))
        diffResult.dispatchUpdatesTo(this@StationTableRecyclerViewAdapter)
        items = newItems*/

        //This if to try to mitigate crashes to some success, real debug would be to understand what happens on
        //screen rotation to the size of the list of items passed
        if (items.size == newItems.size) {
            //...this is faster but lead to crashes like
            //java.lang.IndexOutOfBoundsException: Inconsistency detected. Invalid view holder adapter positionViewHolder
            //probably because of timing a user location update leading to resort while dispatching or something
            coroutineScopeIO.launch {
                //Log.d(TAG, "Background thread, about to calculate diffresult. loadId:$randomDebugloadID oldSize: ${items.size} -- newSize: ${newItems.size}")
                val diffResult = DiffUtil.calculateDiff(TableDiffCallback(items, newItems))

                coroutineScopeMAIN.launch {
                    //Log.d(TAG, "UI thread, about to dispatch. loadId:$randomDebugloadID oldSize: ${items.size} -- newSize: ${newItems.size}")
                    diffResult.dispatchUpdatesTo(this@StationTableRecyclerViewAdapter)
                    items = newItems
                }
            }
        } else {
            notifyDataSetChanged()
            items = newItems
        }
    }

    override fun onBindViewHolder(holder: BikeStationListItemViewHolder, position: Int) {

        holder.bindStation(items[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BikeStationListItemViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.stationtable_item, parent, false)
        return BikeStationListItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class BikeStationListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private var durationText: TextView = itemView.findViewById(R.id.station_proximity)
        private var nameText: TextView = itemView.findViewById(R.id.station_name)
        var availabilityText: TextView = itemView.findViewById(R.id.station_availability)

        private var favoriteFab: FloatingActionButton = itemView.findViewById(R.id.favorite_fab)

        //This View is gone by default. It becomes visible when a row in the recycler View is tapped
        //It's used in two ways
        //-clear the space underneath fabs final positions
        //-anchor fabs to their final position
        var fabsAnchor: FrameLayout = itemView.findViewById(R.id.fabs_anchor)

        private val fabAnimHandler: Handler? = null
        private var stationId: String? = null

        val favoriteFabTarget: ViewTarget
            get() = ViewTarget(favoriteFab)

        init {

            itemView.setOnClickListener(this)

            favoriteFab.setOnClickListener(this)
        }

        fun bindStation(item: StationTableItemData) {

            stationId = item.locationHash

            durationText.visibility = item.durationTextVisibility
            durationText.text = item.durationText

            durationText.alpha = item.proximityAlpha

            nameText.text = item.name

            nameText.alpha = item.nameAlpha

            availabilityText.paint.isStrikeThruText = item.isAvailabilityPaintStrikeThru
            availabilityText.paint.typeface = item.availabilityPaintTypeface


            //TODO: replug fab in item view
            /////////////////////////////////////////////////////////////////////
            //Remove this code to display a fab on selected station
            //val nameWidthPercent: Float

            //TODO: on observinf, is durationText string is null or not
            /*if (mShowProximity) {
                nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true)
            } else {
                nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true) + Utils.getPercentResource(mCtx, R.dimen.proximity_column_width_percent, true)
            }

            //name width percentage restoration
            val params = nameText.layoutParams as PercentRelativeLayout.LayoutParams
            val info = params.percentLayoutInfo
            info.widthPercent = nameWidthPercent
            nameText.requestLayout()*/

            availabilityText.text = item.availabilityText

            availabilityText.alpha = item.availabilityAlpha

            itemView.setBackgroundResource(item.itemBackgroundResId)
        }

        //TODO: model should serve as intercommunication, it will maintain click repsonsiveness
        //state and react accordingly (business logic)
        override fun onClick(view: View) {

            //TODO: rework BikeStation identification
            //I'm on the ViewHolder, I have the bike station location hash/"id"
            tableFragmentModel.setLastClickedStationById(stationId)

            //TODO: see stationTableRecyclerViewAdapter.notifyItemRangeChanged(0, it?.size ?: 0) in tableFragment
            //JAVA CODE ! (Converted in Kotlin)
            /*fun setSelectedPos(pos: Int, unselectedOnTwice: Boolean): Int {

                var toReturn = NO_POSITION

                if (mSelectedPos == pos)
                    if (unselectedOnTwice)
                        clearSelection()
                    else
                        toReturn = mSelectedPos
                else {
                    notifyItemChanged(mSelectedPos)
                    mSelectedPos = pos
                    notifyItemChanged(pos)
                    toReturn = mSelectedPos
                }

                return toReturn
            }*/





            /*when (view.id) {

                R.id.list_item_root ->{ }



                R.id.favorite_fab -> {
                    //must redo binding for favorite name display
                    notifyItemChanged(getStationItemPositionInList(selected!!.locationHash))
                    mListener.onStationListItemClick(StationTableFragment.STATION_LIST_FAVORITE_FAB_CLICK_PATH)
                    //ordering matters
                    //TODO: add table model method to check if id is favorite (call simply forwards to repo)
                    if (mFavoriteSheetListViewModel.isFavorite(selected!!.locationHash))
                        favoriteFab.setImageResource(R.drawable.ic_action_favorite_24dp)
                    else
                        favoriteFab.setImageResource(R.drawable.ic_action_favorite_outline_24dp)
                }
            }
            */
        }
    }

    companion object {

        private val TAG = StationTableRecyclerViewAdapter::class.java.simpleName
    }
}