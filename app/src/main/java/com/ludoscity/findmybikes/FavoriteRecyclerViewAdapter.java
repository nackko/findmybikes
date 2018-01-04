package com.ludoscity.findmybikes;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;
import com.ludoscity.findmybikes.utils.Utils;
import com.ludoscity.findmybikes.viewmodels.FavoriteListViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by F8Full on 2016-03-31.
 * Adapter for the RecyclerView displaying favorites station in a sheet
 * Also allows edition
 * 2016-06-03 partially from - https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.4okwgvgtx
 */
public class FavoriteRecyclerViewAdapter extends RecyclerView.Adapter<FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder>
                                        implements ItemTouchHelperAdapter {


    //following should be final
    private static OnFavoriteListItemClickListener mItemClickListener;
    //following should be final
    private static OnFavoriteListItemStartDragListener mItemStartDragListener;
    private static InputMethodManager mInputMethodManager;

    //Those variable to extract Context data at construction so that ViewHolders can statically reference them
    private static float mFavoriteNameWidthSheetEditing;
    private static float mFavoriteNameWidthNoSheetEditing;
    private static int mResolvedThemeAccentColor;
    private static int mResolvedThemeAccentTransparentColor;

    private static boolean mSheetEditing = false;

    private static LifecycleOwner mOwner;
    private static FavoriteListViewModel mFavoriteListViewModel;

    //TODO: Use ViewModel (Android architecture component)
    //Present self to past self : bingo !
    private List<FavoriteEntityBase> mFavoriteList = new ArrayList<>();

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mFavoriteList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mFavoriteList, i, i - 1);
            }
        }

        //modify persisted data here ?? check what happens when a favorite is removed
        //NO : YOU CAN CANCEL YOUR EDITS, WAIT UNTIL EDIT DONE BUTTON IS TAPPED
        notifyItemMoved(fromPosition, toPosition);
        return true;

    }

    @Override
    public void onItemDismiss(int position) {

    }

    //Model Observer is fragment. It uses this API to update the interface
    public void resetFavoriteList(List<? extends FavoriteEntityBase> newList){
        mFavoriteList.clear();
        mFavoriteList.addAll(newList);

        notifyDataSetChanged();
    }

    public void removeFavorite(String favoriteId, int adapterPosition){
        for (FavoriteEntityBase fav : mFavoriteList){
            if (fav.getId().equalsIgnoreCase(favoriteId)){
                mFavoriteList.remove(fav);
                break;
            }
        }

        notifyItemRemoved(adapterPosition);
    }

    public void setSheetEditing(boolean sheetEditing) {
        mSheetEditing = sheetEditing;
        notifyDataSetChanged();
    }

    public void onFavoriteSheetEditDone() {
        //List<FavoriteEntityBase> newlyOrderedFavList = new ArrayList<>();
        //newlyOrderedFavList.addAll(mFavoriteRecyclerViewAdapter.);

        //TODO: rig UI index to new Room database column
        int i=0;

        for (FavoriteEntityBase fav :
                mFavoriteList) {
            //fav.setUIIndex(i);
            mFavoriteListViewModel.updateFavorite(fav);
            ++i;
        }


        /*mFavoriteRecyclerViewAdapter.clearFavoriteList();

        ListIterator<FavoriteEntityBase> li = newlyOrderedFavList.listIterator(newlyOrderedFavList.size());

        while (li.hasPrevious())
        {
            addFavorite(li.previous(), true, false);
        }*/
    }

    //TODO: investigate making the sheet (and not NearbyActivity) listening and forwarding relevant
    //event to NearbyActivity
    public interface OnFavoriteListItemClickListener {
        void onFavoriteListItemClick(String _stationId);
        void onFavoriteListItemNameEditDone(String _stationId, String _newName );

        void onFavoriteListItemNameEditBegin();

        void onFavoristeListItemNameEditAbort();

        void onFavoriteListItemDelete(String favoriteId, int adapterPosition);
    }

    public interface OnFavoriteListItemStartDragListener{

         void onFavoriteListItemStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public interface FavoriteItemTouchHelperViewHolder {

        /**
         * Called when the ItemTouchHelper first registers an
         * item as being moved or swiped.
         * Implementations should update the item view to indicate
         * it's active state.
         */
        void onItemSelected();


        /**
         * Called when the ItemTouchHelper has completed the
         * move or swipe, and the active item state should be cleared.
         */
        void onItemClear();
    }

    public FavoriteRecyclerViewAdapter(OnFavoriteListItemClickListener _onItemClicklistener,
                                       OnFavoriteListItemStartDragListener _onItemDragListener, Context _ctx,
                                       FavoriteListViewModel favListViewModel, LifecycleOwner _owner){
        super();
        mItemClickListener = _onItemClicklistener;
        mItemStartDragListener = _onItemDragListener;
        mFavoriteNameWidthSheetEditing = Utils.getPercentResource(_ctx, R.dimen.favorite_name_width_sheet_editing, true);
        mFavoriteNameWidthNoSheetEditing = Utils.getPercentResource(_ctx, R.dimen.favorite_name_width_no_sheet_editing, true);
        mResolvedThemeAccentColor = ContextCompat.getColor(_ctx, R.color.theme_accent);
        mResolvedThemeAccentTransparentColor = ContextCompat.getColor(_ctx, R.color.theme_accent_transparent);
        mInputMethodManager = (InputMethodManager) _ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        mFavoriteListViewModel = favListViewModel;
        mOwner = _owner;
    }

    @Override
    public FavoriteListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.favoritelist_item, parent, false);
        return new FavoriteListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FavoriteListItemViewHolder holder, int position) {
        holder.bindFavorite(mFavoriteList.get(position));

    }

    @Override
    public int getItemCount() {
        return mFavoriteList.size();
    }

    public static class FavoriteListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnFocusChangeListener, View.OnTouchListener, FavoriteItemTouchHelperViewHolder {

        TextView mName;
        String mFavoriteId;
        FloatingActionButton mEditFab;
        FloatingActionButton mDoneFab;
        FloatingActionButton mDeleteFab = (FloatingActionButton) itemView.findViewById(R.id.favorite_delete_fab);
        ImageView mOrderingAffordanceHandle;

        boolean mEditing = false;
        String mNameBeforeEdit;

        public String getFavoriteId(){ return mFavoriteId; }

        FavoriteListItemViewHolder(View itemView) {
            super(itemView);

            mName = itemView.findViewById(R.id.favorite_name);
            mEditFab = itemView.findViewById(R.id.favorite_name_edit_fab);
            mDoneFab = itemView.findViewById(R.id.favorite_name_done_fab);

            mOrderingAffordanceHandle = itemView.findViewById(R.id.reorder_affordance_handle);
            mOrderingAffordanceHandle.setOnTouchListener(this);

            mName.setOnClickListener(this);
            mEditFab.setOnClickListener(this);
            mDoneFab.setOnClickListener(this);
            mDeleteFab.setOnClickListener(this);
        }

        void bindFavorite(FavoriteEntityBase _favorite){

            mFavoriteListViewModel.getFavoriteEntityStationForId(_favorite.getId()).observe(mOwner, new Observer<FavoriteEntityStation>() {
                @Override
                public void onChanged(@Nullable FavoriteEntityStation favoriteEntityStation) {

                    if (favoriteEntityStation == null)  //Our favorite been deleted, fragment observer code will take care of that
                        return;

                    if (favoriteEntityStation.isDisplayNameDefault())
                        mName.setTypeface(null, Typeface.ITALIC);
                    else
                        mName.setTypeface(null, Typeface.BOLD);

                    mName.setText(favoriteEntityStation.getDisplayName());
                    mFavoriteId = favoriteEntityStation.getId();

                    itemView.setBackgroundResource(R.color.theme_accent_transparent);


                    if (mSheetEditing){
                        mEditFab.hide(new FloatingActionButton.OnVisibilityChangedListener(){
                            @Override
                            public void onHidden(FloatingActionButton fab) {
                                super.onHidden(fab);
                                mEditFab.setVisibility(View.INVISIBLE);
                            }
                        });

                        mDeleteFab.show();

                        mOrderingAffordanceHandle.setVisibility(View.VISIBLE);

                        //The width percentage is updated so that the name TextView gives room to the fabs
                        //RecyclerView gives us free opacity/bounds resizing animations
                        PercentRelativeLayout.LayoutParams params =(PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
                        PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();

                        info.widthPercent = mFavoriteNameWidthSheetEditing;
                        mName.requestLayout();

                    }
                    else {
                        mDeleteFab.hide(new FloatingActionButton.OnVisibilityChangedListener(){
                            @Override
                            public void onHidden(FloatingActionButton fab) {
                                super.onHidden(fab);
                                mDeleteFab.setVisibility(View.INVISIBLE);
                            }
                        });
                        mEditFab.show();

                        mOrderingAffordanceHandle.setVisibility(View.GONE);

                        PercentRelativeLayout.LayoutParams params =(PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
                        PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();

                        info.widthPercent = mFavoriteNameWidthNoSheetEditing;
                        mName.requestLayout();
                    }

                }
            });



            //Beware FloatingActionButton bugs !!
            //so, to get nicely animated buttons I need
            // - 1ms delay (using Handler)
            // - set button visibility manualy to invisible at the end of the hiding animation
            //(using fab provided animation interface)
            /*Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (mSheetEditing){
                        mEditFab.hide(new FloatingActionButton.OnVisibilityChangedListener(){
                            @Override
                            public void onHidden(FloatingActionButton fab) {
                                super.onHidden(fab);
                                mEditFab.setVisibility(View.INVISIBLE);
                            }
                        });

                        mDeleteFab.show();

                        mOrderingAffordanceHandle.setVisibility(View.VISIBLE);

                        //The width percentage is updated so that the name TextView gives room to the fabs
                        //RecyclerView gives us free opacity/bounds resizing animations
                        PercentRelativeLayout.LayoutParams params =(PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
                        PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();

                        info.widthPercent = Utils.getPercentResource(mCtx, R.dimen.favorite_name_width_sheet_editing, true);
                        mName.requestLayout();

                    }
                    else {
                        mDeleteFab.hide(new FloatingActionButton.OnVisibilityChangedListener(){
                            @Override
                            public void onHidden(FloatingActionButton fab) {
                                super.onHidden(fab);
                                mDeleteFab.setVisibility(View.INVISIBLE);
                            }
                        });
                        mEditFab.show();

                        mOrderingAffordanceHandle.setVisibility(View.GONE);

                        PercentRelativeLayout.LayoutParams params =(PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
                        PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();

                        info.widthPercent = Utils.getPercentResource(mCtx, R.dimen.favorite_name_width_no_sheet_editing, true);
                        mName.requestLayout();
                    }

                }
            }, 1);*/

        }

        @Override
        public void onClick(View v) {

            if (mSheetEditing && v.getId() != R.id.favorite_delete_fab)
                return;

            switch (v.getId()){
                case R.id.favorite_name:
                    if (!mEditing)
                        mItemClickListener.onFavoriteListItemClick(mFavoriteId);
                    else //User pressed back to hide keyboard
                        showSoftInput();
                    break;

                case R.id.favorite_name_edit_fab:
                    mEditing = true;
                    setupItemEditMode(true);
                    mItemClickListener.onFavoriteListItemNameEditBegin();
                    break;

                case R.id.favorite_name_done_fab:
                    mEditing = false;
                    setupItemEditMode(false);
                    mItemClickListener.onFavoriteListItemNameEditDone(mFavoriteId, mName.getText().toString());
                    break;

                case R.id.favorite_delete_fab:
                    mItemClickListener.onFavoriteListItemDelete(mFavoriteId, getAdapterPosition());
                    break;
            }
        }

        private void setupItemEditMode(boolean _editing) {
            if (_editing)
            {
                mEditFab.hide();
                mDoneFab.show();

                mName.setCursorVisible(true);
                mName.setOnFocusChangeListener(this);
                mName.setTextIsSelectable(true);
                mName.setFocusableInTouchMode(true);
                mName.requestFocus();

                //API level 21+
                //mName.setShowSoftInputOnFocus(true);

                showSoftInput();
            }
            else {
                hideSoftInput();

                mName.setCursorVisible(false);
                mName.setOnFocusChangeListener(null);
                mName.setTextIsSelectable(false);
                mName.setFocusableInTouchMode(false);

                String newName = mName.getText().toString().trim();

                if (!newName.isEmpty())
                    mName.setText(newName);
                else
                    //restoring original name
                    mName.setText(mNameBeforeEdit);

                mDoneFab.hide();
                mEditFab.show();
            }
        }

        private void showSoftInput() {
            mInputMethodManager.showSoftInput(mName, InputMethodManager.SHOW_FORCED);
        }

        private void hideSoftInput() {
            mInputMethodManager.hideSoftInputFromWindow(mName.getWindowToken(), 0);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {

            TextView vTV = (TextView)v;

            if (hasFocus){

                mNameBeforeEdit = vTV.getText().toString();

            } else {

                if (mEditing) {
                    //Editing mode wasn't left from clicking done fab, restoring original name
                    vTV.setText(mNameBeforeEdit);

                    mEditing = false;
                    setupItemEditMode(false);
                    mItemClickListener.onFavoristeListItemNameEditAbort();
                }
            }
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){

                mItemStartDragListener.onFavoriteListItemStartDrag(this);
            }
            return false;
        }

        @Override
        public void onItemSelected() {

            if (mSheetEditing)
                animateBackgroundColor(mResolvedThemeAccentTransparentColor, mResolvedThemeAccentColor, 250);
        }

        @Override
        public void onItemClear() {
            if (mSheetEditing)
                animateBackgroundColor(mResolvedThemeAccentColor, mResolvedThemeAccentTransparentColor, 250);
        }

        //http://stackoverflow.com/questions/2614545/animate-change-of-view-background-color-on-android/14467625#14467625
        private void animateBackgroundColor(int _colorFrom, int _colorTo, int _durationMillisecond){
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), _colorFrom, _colorTo);
            colorAnimation.setDuration(_durationMillisecond);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    itemView.setBackgroundColor((int) valueAnimator.getAnimatedValue());
                }
            });
            colorAnimation.start();
        }
    }
}
