package com.ludoscity.findmybikes.ui.table

import android.content.Context
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.viewmodels.FavoriteListViewModel

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Adapter used to show the datas of every stationItem
 */
class StationTableRecyclerViewAdapter(private val mListener: OnStationListItemClickListener,
                                      private val mCtx: Context, favListViewModel: FavoriteListViewModel) : RecyclerView.Adapter<StationTableRecyclerViewAdapter.BikeStationListItemViewHolder>() {

    var items: List<StationTableItemData> = emptyList()

    //TODO: selected pos in model
    //var selectedPos = NO_POSITION
    //private set

    //TODO: rework the fab
    //private var mFabAnimationRequested = false


    fun loadItems(newItems: List<StationTableItemData>) {
        items = newItems
    }

    /*fun notifyStationChanged(_stationId: String) {
        TODO("not implemented")
        notifyItemChanged(getStationItemPositionInList(_stationId))
    }*/

    interface OnStationListItemClickListener {
        fun onStationListItemClick(_path: String)
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

        var mProximity: TextView
        var mName: TextView
        var mAvailability: TextView

        var mFavoriteFab: FloatingActionButton

        //This View is gone by default. It becomes visible when a row in the recycler View is tapped
        //It's used in two ways
        //-clear the space underneath fabs final positions
        //-anchor fabs to their final position
        var mFabsAnchor: FrameLayout

        private val mFabAnimHandler: Handler? = null
        private var mStationId: String? = null

        val favoriteFabTarget: ViewTarget
            get() = ViewTarget(mFavoriteFab)

        init {

            mProximity = itemView.findViewById(R.id.station_proximity)
            mName = itemView.findViewById(R.id.station_name)
            mAvailability = itemView.findViewById(R.id.station_availability)

            mFavoriteFab = itemView.findViewById(R.id.favorite_fab)
            mFabsAnchor = itemView.findViewById(R.id.fabs_anchor)
            itemView.setOnClickListener(this)

            mFavoriteFab.setOnClickListener(this)
        }

        fun bindStation(item: StationTableItemData) {

            mStationId = item.locationHash

            if (item.proximity != null) {
                mProximity.visibility = View.VISIBLE
                mProximity.text = item.proximity
            } else {
                mProximity.visibility = View.GONE
            }

            mProximity.alpha = item.proximityAlpha

            mName.text = item.name

            mName.alpha = item.nameAlpha


            //TODO: replug fab in item view
            /////////////////////////////////////////////////////////////////////
            //Remove this code to display a fab on selected station
            //val nameWidthPercent: Float

            //TODO: on observinf, is proximity string is null or not
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

            /*when (view.id) {

                R.id.list_item_root ->

                    if (!mRespondToClick) {
                        mListener.onStationListItemClick(StationTableFragment.STATION_LIST_INACTIVE_ITEM_CLICK_PATH)
                    } else {

                        val oldSelectedPos = selectedPos

                        this@StationTableRecyclerViewAdapter.setSelection(mStationId, false)

                        if (oldSelectedPos != selectedPos) {

                            mListener.onStationListItemClick(StationTableFragment.STATION_LIST_ITEM_CLICK_PATH)
                            requestFabAnimation()
                        }
                    }

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