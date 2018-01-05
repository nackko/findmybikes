package com.ludoscity.findmybikes.fragments;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ludoscity.findmybikes.FavoriteRecyclerViewAdapter;
import com.ludoscity.findmybikes.ItemTouchHelperAdapter;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityPlace;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;
import com.ludoscity.findmybikes.helpers.FavoriteRepository;
import com.ludoscity.findmybikes.utils.DividerItemDecoration;
import com.ludoscity.findmybikes.utils.ScrollingLinearLayoutManager;
import com.ludoscity.findmybikes.viewmodels.FavoriteListViewModel;
import com.ludoscity.findmybikes.viewmodels.NearbyActivityViewModel;

import java.util.List;

public class FavoriteListFragment extends Fragment implements
        FavoriteRecyclerViewAdapter.OnFavoriteListItemStartDragListener,//TODO: investigate making the sheet listening and forwarding
        FavoriteRecyclerViewAdapter.OnFavoriteListItemClickListener{

    public static final String FAVORITE_LIST_ITEM_CLICK_PATH = "station_list_item_click";
    public static final String FAVORITE_LIST_INACTIVE_ITEM_CLICK_PATH = "station_list_inactive_item_click";

    //private EditableMaterialSheetFab mFavoritesSheetFab;
    private NearbyActivityViewModel mNearbyActivityViewModel;
    private FavoriteListViewModel mFavoriteListViewModel;
    private ItemTouchHelper mFavoriteItemTouchHelper;
    FavoriteRecyclerViewAdapter mFavoriteRecyclerViewAdapter;

    private OnFavoriteListFragmentInteractionListener mListener;
    private boolean mIsFavoriteSheetEditInProgress = false;
    private boolean mIsFavoriteSheetItemNameEditInProgress = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflatedView =  inflater.inflate(R.layout.fragment_favorite_list, container, false);

        //ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
        //        ItemTouchHelper.LEFT) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback( ItemTouchHelper.UP | ItemTouchHelper.DOWN
                , ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                ((ItemTouchHelperAdapter)recyclerView.getAdapter()).onItemMove(viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                onFavoriteListItemDelete(((FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder)viewHolder).getFavoriteId(), viewHolder.getAdapterPosition());
            }

            @Override
            public boolean isLongPressDragEnabled() {

                return mNearbyActivityViewModel.isFavoriteSheetEditInProgress().getValue();//mFavoriteRecyclerViewAdapter.getSheetEditing();

            }

            @Override
            public boolean isItemViewSwipeEnabled() {

                return !mIsFavoriteSheetEditInProgress && !mIsFavoriteSheetItemNameEditInProgress;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder _viewHolder, int _actionState){
                if (_actionState != ItemTouchHelper.ACTION_STATE_IDLE){
                    if (_viewHolder instanceof FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder){
                        FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder favoriteItemViewHolder =
                                (FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder) _viewHolder;
                        favoriteItemViewHolder.onItemSelected();
                    }
                }
                super.onSelectedChanged(_viewHolder, _actionState);
            }

            @Override
            public void clearView(RecyclerView _recyclerView, RecyclerView.ViewHolder _viewHolder){
                super.clearView(_recyclerView, _viewHolder);
                if (_viewHolder instanceof FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder){
                    FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder favoriteItemViewHolder =
                            (FavoriteRecyclerViewAdapter.FavoriteItemTouchHelperViewHolder) _viewHolder;
                    favoriteItemViewHolder.onItemClear();
                }
            }
        };

        mFavoriteItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);

        RecyclerView favoriteRecyclerView = inflatedView.findViewById(R.id.favorite_list_recyclerview);

        favoriteRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        favoriteRecyclerView.setLayoutManager(new ScrollingLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false, 300));

        mFavoriteListViewModel = ViewModelProviders.of(this).get(FavoriteListViewModel.class);
        mFavoriteListViewModel.getFavoriteEntityStationList().observe(this, new Observer<List<FavoriteEntityStation>>() {
            @Override
            public void onChanged(@Nullable List<FavoriteEntityStation> favoriteEntityStations) {
                //TODO: have more elaborate code, right now when a single item is changed, both this and the individual holders get notified
                //TODO: maybe add UI index comparison, or that will be taken care of by individual items observer pattern ?
                if (favoriteEntityStations.size() > mFavoriteRecyclerViewAdapter.getItemCount()) {
                    mFavoriteRecyclerViewAdapter.resetFavoriteList(favoriteEntityStations);
                }

                //TODO: investigate communicating the required state change through NearbyActivityViewModel
                //actually that should be done now by also observing the same model
                mListener.onFavoriteListChanged(favoriteEntityStations.size() == 0);
            }
        });

        mFavoriteRecyclerViewAdapter = new FavoriteRecyclerViewAdapter( this, this, getActivity().getApplicationContext(), mFavoriteListViewModel, this);

        favoriteRecyclerView.setAdapter(mFavoriteRecyclerViewAdapter);

        mFavoriteItemTouchHelper.attachToRecyclerView(favoriteRecyclerView);

        return inflatedView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mNearbyActivityViewModel = ViewModelProviders.of(getActivity()).get(NearbyActivityViewModel.class);

        mNearbyActivityViewModel.isFavoriteSheetEditInProgress().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean favSheetEditInProgress) {

                mIsFavoriteSheetEditInProgress = favSheetEditInProgress;
                mFavoriteRecyclerViewAdapter.setSheetEditing(mIsFavoriteSheetEditInProgress);
            }
        });

        mNearbyActivityViewModel.isFavoriteSheetItemNameEditInProgress().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean favItemNameEditInProgress) {

                mIsFavoriteSheetItemNameEditInProgress = favItemNameEditInProgress;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFavoriteListFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onFavoriteListItemClick(String _stationId) {

        //TODO: act on NearbyActivityViewModel instead of a direct callback
        mListener.onFavoriteListItemClicked(_stationId);
    }

    @Override
    public void onFavoriteListItemNameEditDone(String _favoriteId, String _newName) {

        if (!_favoriteId.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {

            FavoriteEntityBase fav = mFavoriteListViewModel.getFavoriteEntityForId(_favoriteId);
            fav.setCustomName(_newName);
            mFavoriteListViewModel.updateFavorite(fav);

            mListener.onFavoriteItemEditDone(_favoriteId);
        }
        else{
            FavoriteEntityBase favEntity = mFavoriteListViewModel.getFavoriteEntityForId(_favoriteId);
            CharSequence attr = favEntity.getAttributions();
            String attrString = "";
            if (attr != null)
                attrString = attr.toString();

            mFavoriteListViewModel.addFavorite(new FavoriteEntityPlace(favEntity.getId(), _newName, favEntity.getLocation(), attrString));
        }

        mNearbyActivityViewModel.showFavoriteSheetEditFab();
        mNearbyActivityViewModel.favoriteItemNameEditStop();
    }

    @Override
    public void onFavoriteListItemNameEditBegin() {
        mNearbyActivityViewModel.favoriteItemNameEditStart();

    }

    @Override
    public void onFavoristeListItemNameEditAbort() {

        //mFavoritesSheetFab.showEditFab();
        mNearbyActivityViewModel.favoriteItemNameEditStop();
        //mFavoriteItemEditInProgress = false;
    }

    @Override
    public void onFavoriteListItemDelete(String favoriteId, int adapterPosition) {
        mFavoriteRecyclerViewAdapter.removeFavorite(favoriteId, adapterPosition);

        mListener.onFavoriteItemDeleted(favoriteId, false);
        mFavoriteListViewModel.removeFavorite(favoriteId);
    }

    @Override
    public void onFavoriteListItemStartDrag(RecyclerView.ViewHolder _viewHolder){
        mFavoriteItemTouchHelper.startDrag(_viewHolder);
    }

    public interface OnFavoriteListFragmentInteractionListener {

        void onFavoriteItemEditDone(String fsvoriteId);
        void onFavoriteItemDeleted(String favoriteId, boolean showUndo);
        void onFavoriteListChanged(boolean noFavorite);

        void onFavoriteListItemClicked(String favoriteId);
    }

}
