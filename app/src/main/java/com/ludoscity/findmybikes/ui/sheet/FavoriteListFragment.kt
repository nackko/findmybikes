package com.ludoscity.findmybikes.ui.sheet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivityViewModel
import com.ludoscity.findmybikes.utils.InjectorUtils

class FavoriteListFragment : Fragment(), FavoriteRecyclerViewAdapter.OnFavoriteListItemStartDragListener, //TODO: investigate making the sheet listening and forwarding
        FavoriteRecyclerViewAdapter.OnFavoriteListItemClickListener, EditableMaterialSheetFab.OnFavoriteSheetEventListener {

    //private EditableMaterialSheetFab mFavoritesSheetFab;
    private var findMyBikesActivityViewModel: FindMyBikesActivityViewModel? = null
    private var favoriteSheetListViewModel: FavoriteSheetListViewModel? = null
    private var favoriteItemTouchHelper: ItemTouchHelper? = null
    private lateinit var favoriteRecyclerViewAdapter: FavoriteRecyclerViewAdapter
    private lateinit var favoriteRecyclerView: RecyclerView

    private var listener: OnFavoriteListFragmentInteractionListener? = null
    private var isFavoriteSheetEditInProgress = false
    private var isFavoriteSheetItemNameEditInProgress = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val inflatedView = inflater.inflate(R.layout.fragment_favorite_list, container, false)

        favoriteRecyclerView = inflatedView.findViewById(R.id.favorite_list_recyclerview)

        return inflatedView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        findMyBikesActivityViewModel = ViewModelProviders.of(activity!!).get(FindMyBikesActivityViewModel::class.java)

        findMyBikesActivityViewModel!!.isFavoriteSheetItemNameEditInProgress.observe(
                this,
                Observer { favItemNameEditInProgress ->
                    isFavoriteSheetItemNameEditInProgress = favItemNameEditInProgress!!
                })

        val facto = InjectorUtils.provideFavoriteSheetListFragmentViewModelFactory(activity!!.application, findMyBikesActivityViewModel!!.isFavoriteSheetEditInProgress)

        favoriteSheetListViewModel = ViewModelProviders.of(this, facto).get(FavoriteSheetListViewModel::class.java)

        favoriteSheetListViewModel!!.sheetItemDataList.observe(this, Observer { favoriteSheetItemData -> favoriteRecyclerViewAdapter.loadItems(favoriteSheetItemData!!) })

        favoriteRecyclerViewAdapter = FavoriteRecyclerViewAdapter(this, this, activity!!.applicationContext)

        favoriteRecyclerView.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL_LIST))
        favoriteRecyclerView.layoutManager = ScrollingLinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, false, 300)

        //ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
        //        ItemTouchHelper.LEFT) {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                (recyclerView.adapter as ItemTouchHelperAdapter).onItemMove(viewHolder.adapterPosition,
                        target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                onFavoriteListItemDelete((viewHolder as FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder).favoriteId!!, viewHolder.getAdapterPosition())
            }

            override fun isLongPressDragEnabled(): Boolean {
                return findMyBikesActivityViewModel!!.isFavoriteSheetEditInProgress.value!!
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return !isFavoriteSheetEditInProgress && !isFavoriteSheetItemNameEditInProgress
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (viewHolder is FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder) {
                        val favoriteItemViewHolder = viewHolder as FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder?
                        favoriteItemViewHolder!!.onItemSelected()
                    }
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (viewHolder is FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder) {
                    val favoriteItemViewHolder = viewHolder as FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder
                    favoriteItemViewHolder.onItemClear()
                }
            }
        }

        favoriteItemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)

        favoriteRecyclerView.adapter = favoriteRecyclerViewAdapter

        favoriteItemTouchHelper!!.attachToRecyclerView(favoriteRecyclerView)

        try {
            listener = activity as OnFavoriteListFragmentInteractionListener?
        } catch (e: Exception) {
            throw ClassCastException(activity!!.toString() + " must implement OnFragmentInteractionListener")
        }

    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onFavoriteListItemClick(favoriteId: String) {
        findMyBikesActivityViewModel!!.setLastClickedFavoriteListItemFavoriteId(favoriteId)
    }

    override fun onFavoriteListItemNameEditDone(favoriteId: String, newName: String) {

        if (!newName.isEmpty()) {
            favoriteSheetListViewModel!!.updateFavoriteCustomNameByFavoriteId(favoriteId, newName)
            findMyBikesActivityViewModel!!.refreshStationAAndB()
        }

        findMyBikesActivityViewModel!!.showFavoriteSheetEditFab()
        findMyBikesActivityViewModel!!.favoriteItemNameEditStop()
    }

    override fun onFavoriteListItemNameEditBegin() {
        findMyBikesActivityViewModel!!.favoriteItemNameEditStart()
    }

    override fun onFavoristeListItemNameEditAbort() {
        findMyBikesActivityViewModel!!.favoriteItemNameEditStop()
    }

    override fun onFavoriteListItemDelete(favoriteId: String, adapterPosition: Int) {
        favoriteSheetListViewModel!!.removeFavorite(favoriteId)
    }

    override fun onFavoriteListItemStartDrag(viewHolder: RecyclerView.ViewHolder) {
        favoriteItemTouchHelper!!.startDrag(viewHolder)
    }

    override fun onFavoriteSheetEditDone() {

        for ((i, fav) in favoriteRecyclerViewAdapter.items.withIndex()) {
            favoriteSheetListViewModel!!.updateFavoriteUiIndexByFavoriteId(fav.favoriteId, i)
        }
        findMyBikesActivityViewModel!!.favoriteSheetEditDone()
    }

    override fun onFavoriteSheetEditCancel() {
    }

    interface OnFavoriteListFragmentInteractionListener {

        fun onFavoriteItemDeleted(favoriteId: String, showUndo: Boolean)
        fun onFavoriteListChanged(noFavorite: Boolean)

        fun onFavoriteListItemClicked(favoriteId: String)
    }

    companion object {

        val FAVORITE_LIST_ITEM_CLICK_PATH = "station_list_item_click"
        val FAVORITE_LIST_INACTIVE_ITEM_CLICK_PATH = "station_list_inactive_item_click"
    }

}
