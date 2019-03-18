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
import com.ludoscity.findmybikes.viewmodels.FavoriteListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Adapter used to show the datas of every stationItem
 */
class StationTableRecyclerViewAdapter(private val tableFragmentModel: TableFragmentViewModel,
                                      favListViewModel: FavoriteListViewModel) : RecyclerView.Adapter<StationTableRecyclerViewAdapter.BikeStationListItemViewHolder>() {

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

    var items: List<StationTableItemData> = emptyList()

    //TODO: selected pos in model
    //var selectedPos = NO_POSITION
    //private set

    //TODO: rework the fab
    //private var mFabAnimationRequested = false

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)
    private val coroutineScopeMAIN = CoroutineScope(Dispatchers.Main)

    fun loadItems(newItems: List<StationTableItemData>) {

        coroutineScopeIO.launch {
            val diffResult = DiffUtil.calculateDiff(TableDiffCallback(items, newItems))

            coroutineScopeMAIN.launch {
                diffResult.dispatchUpdatesTo(this@StationTableRecyclerViewAdapter)
                items = newItems
            }
        }
    }

    fun notifyStationChanged(_stationId: String) {
        TODO("not implemented")
        //notifyItemChanged(getStationItemPositionInList(_stationId))
    }


    init {
        //TODO: should be favorite repo
        //TODO: used to build display name, which is model responsibility now
        mFavoriteListViewModel = favListViewModel
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

        var mProximity: TextView = itemView.findViewById(R.id.station_proximity)
        var mName: TextView = itemView.findViewById(R.id.station_name)
        var mAvailability: TextView = itemView.findViewById(R.id.station_availability)

        var mFavoriteFab: FloatingActionButton = itemView.findViewById(R.id.favorite_fab)

        //This View is gone by default. It becomes visible when a row in the recycler View is tapped
        //It's used in two ways
        //-clear the space underneath fabs final positions
        //-anchor fabs to their final position
        var mFabsAnchor: FrameLayout = itemView.findViewById(R.id.fabs_anchor)

        private val mFabAnimHandler: Handler? = null
        private var mStationId: String? = null

        val favoriteFabTarget: ViewTarget
            get() = ViewTarget(mFavoriteFab)

        init {

            itemView.setOnClickListener(this)

            mFavoriteFab.setOnClickListener(this)
        }

        fun bindStation(item: StationTableItemData) {

            mStationId = item.locationHash

            if (item.proximityText != null) {
                mProximity.visibility = View.VISIBLE
                mProximity.text = item.proximityText
            } else {
                mProximity.visibility = View.GONE
            }

            mProximity.alpha = item.proximityAlpha

            mName.text = item.name

            mName.alpha = item.nameAlpha

            mAvailability.paint.isStrikeThruText = item.isAvailabilityPaintStrikeThru
            mAvailability.paint.typeface = item.availabilityPaintTypeface


            //TODO: replug fab in item view
            /////////////////////////////////////////////////////////////////////
            //Remove this code to display a fab on selected station
            //val nameWidthPercent: Float

            //TODO: on observinf, is proximityText string is null or not
            /*if (mShowProximity) {
                nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true)
            } else {
                nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true) + Utils.getPercentResource(mCtx, R.dimen.proximity_column_width_percent, true)
            }

            //name width percentage restoration
            val params = mName.layoutParams as PercentRelativeLayout.LayoutParams
            val info = params.percentLayoutInfo
            info.widthPercent = nameWidthPercent
            mName.requestLayout()*/

            mAvailability.text = item.availabilityText

            mAvailability.alpha = item.availabilityalpha

            itemView.setBackgroundResource(item.itemBackgroundResId)
        }

        //TODO: model should serve as intercommunication, it will maintain click repsonsiveness
        //state and react accordingly (business logic)
        override fun onClick(view: View) {

            //TODO: rework BikeStation identification
            //I'm on the ViewHolder, I have the bike station location hash/"id"
            tableFragmentModel.setLastClickedStationById(mStationId)

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
                    if (mFavoriteListViewModel.isFavorite(selected!!.locationHash))
                        mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_24dp)
                    else
                        mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_outline_24dp)
                }
            }
            */
        }
    }

    companion object {

        private lateinit var mFavoriteListViewModel: FavoriteListViewModel
    }
}