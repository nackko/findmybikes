package com.ludoscity.findmybikes.ui.sheet

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.os.CountDownTimer
import android.support.design.widget.FloatingActionButton
import android.support.percent.PercentRelativeLayout
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import com.dinuscxj.progressbar.CircleProgressBar
import com.dmitrymalkovich.android.ProgressFloatingActionButton
import com.ludoscity.findmybikes.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by F8Full on 2016-03-31.
 * Adapter for the RecyclerView displaying favorites station in a sheet
 * Also allows edition
 * 2016-06-03 partially from - https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.4okwgvgtx
 */

//TODO: the business code is strong in this once
class FavoriteRecyclerViewAdapter(itemClickListener: OnFavoriteListItemClickListener,
                                  itemStartDragListener: OnFavoriteListItemStartDragListener, ctx: Context) : RecyclerView.Adapter<FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder>(), ItemTouchHelperAdapter {

    /**
     * Created by F8Full on 2019-03-30.
     *
     * DiffCallback class to compare new items dataset and turn differences into instructions for the recyclerview
     * see : https://developer.android.com/reference/android/support/v7/util/DiffUtil
     */
    private inner class SheetDiffCallback(private val mOldList: List<FavoriteSheetItemData>, private val mNewList: List<FavoriteSheetItemData>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return mOldList.size
        }

        override fun getNewListSize(): Int {
            return mNewList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return mNewList[newItemPosition].favoriteId == mOldList[oldItemPosition].favoriteId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return mNewList[newItemPosition] == mOldList[oldItemPosition]
        }
    }

    var items: List<FavoriteSheetItemData> = emptyList()
        private set

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)
    private val coroutineScopeMAIN = CoroutineScope(Dispatchers.Main)

    fun loadItems(newItems: List<FavoriteSheetItemData>) {
        coroutineScopeIO.launch {
            val diffResult = DiffUtil.calculateDiff(SheetDiffCallback(items, newItems))

            coroutineScopeMAIN.launch {
                diffResult.dispatchUpdatesTo(this@FavoriteRecyclerViewAdapter)
                items = newItems
            }
        }
    }

    override fun onBindViewHolder(holder: FavoriteListItemViewHolder, position: Int) {
        holder.bindFavorite(items[position])

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteListItemViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.favoritelist_item, parent, false)
        return FavoriteListItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /////////////////////////////////////////3
    //old code

    //Should happen in model, swap will be in display data, recyclerview will reflect it through diffutil
    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }

        //modify persisted data here ?? check what happens when a favorite is removed
        //NO : YOU CAN CANCEL YOUR EDITS, WAIT UNTIL EDIT DONE BUTTON IS TAPPED
        notifyItemMoved(fromPosition, toPosition)
        return true

    }

    override fun onItemDismiss(position: Int) {

    }

    //TODO: investigate making the sheet (and not NearbyActivity) listening and forwarding relevant
    //event to NearbyActivity
    //TODO: no, forward to model, which one is still up in the air
    interface OnFavoriteListItemClickListener {
        fun onFavoriteListItemClick(favoriteId: String)
        fun onFavoriteListItemNameEditDone(favoriteId: String, newName: String)

        fun onFavoriteListItemNameEditBegin()

        fun onFavoristeListItemNameEditAbort()

        fun onFavoriteListItemDelete(favoriteId: String, adapterPosition: Int)
    }

    interface OnFavoriteListItemStartDragListener {

        fun onFavoriteListItemStartDrag(viewHolder: RecyclerView.ViewHolder)
    }

    interface FavoriteItemTouchHelperViewHolder {

        /**
         * Called when the ItemTouchHelper first registers an
         * item as being moved or swiped.
         * Implementations should update the item view to indicate
         * it's active state.
         */
        fun onItemSelected()


        /**
         * Called when the ItemTouchHelper has completed the
         * move or swipe, and the active item state should be cleared.
         */
        fun onItemClear()
    }

    init {
        mItemClickListener = itemClickListener
        mItemStartDragListener = itemStartDragListener
        mResolvedThemeAccentColor = ContextCompat.getColor(ctx, R.color.theme_accent)
        mResolvedThemeAccentTransparentColor = ContextCompat.getColor(ctx, R.color.theme_accent_transparent)
        mInputMethodManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mFavoriteItemDeleteCancelDurationMillis = ctx.resources.getInteger(R.integer.favorite_item_delete_cancel_duraiton_millis)
    }

    inner class FavoriteListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnFocusChangeListener, View.OnTouchListener, FavoriteItemTouchHelperViewHolder {

        private var name: TextView = itemView.findViewById(R.id.favorite_name)
        var favoriteId: String? = null
            private set

        private var mEditFab: FloatingActionButton = itemView.findViewById(R.id.favorite_name_edit_fab)
        private var mDoneFab: FloatingActionButton = itemView.findViewById(R.id.favorite_name_done_fab)
        private var mDeleteFab: FloatingActionButton = itemView.findViewById(R.id.favorite_delete_fab)
        private var mDeleteCancelProgressFab: ProgressFloatingActionButton = itemView.findViewById(R.id.favorite_cancel_delete_progressfab)
        private var mDeleteCancelFab: FloatingActionButton = itemView.findViewById(R.id.favorite_cancel_delete_fab)
        private var mDeleteCancelCircleProgressBar: CircleProgressBar = itemView.findViewById(R.id.favorite_cancel_delete_progressbar)
        private var mDeleteCancelCountDownTimer: CountDownTimer

        private var mOrderingAffordanceHandle: ImageView = itemView.findViewById(R.id.reorder_affordance_handle)

        private var mEditing = false
        private var mNameBeforeEdit: String = "null"

        init {

            mOrderingAffordanceHandle.setOnTouchListener(this)

            name.setOnClickListener(this)
            mEditFab.setOnClickListener(this)
            mDoneFab.setOnClickListener(this)
            mDeleteFab.setOnClickListener(this)
            mDeleteCancelFab.setOnClickListener(this)

            mDeleteCancelCountDownTimer = object : CountDownTimer(mFavoriteItemDeleteCancelDurationMillis.toLong(), 17) {
                //60 frames a second = one frame each 17 ms roughly
                override fun onTick(millisUntilFinished: Long) {
                    val progressPercentage = ((mFavoriteItemDeleteCancelDurationMillis.toFloat() - millisUntilFinished.toFloat()) / (mFavoriteItemDeleteCancelDurationMillis / 100f)).toInt()
                    mDeleteCancelCircleProgressBar.progress = progressPercentage
                    name.alpha = (100f - progressPercentage.toFloat()) / 100f
                }

                override fun onFinish() {
                    //This is glitchy but required for now
                    //TODO: refactor item layout xml file with ContraintLayout
                    mDeleteCancelProgressFab.visibility = View.INVISIBLE
                    mDeleteFab.visibility = View.VISIBLE
                    mOrderingAffordanceHandle.visibility = View.VISIBLE
                    name.alpha = 1f
                    mItemClickListener.onFavoriteListItemDelete(favoriteId!!, adapterPosition)
                }
            }
        }

        fun bindFavorite(fav: FavoriteSheetItemData) {
            favoriteId = fav.favoriteId

            name.setTypeface(null, fav.nameTypefaceStyle)

            name.text = fav.nameText

            //The width percentage is updated so that the name TextView gives room to the fabs
            //RecyclerView gives us free opacity/bounds resizing animations
            val params = name.layoutParams as PercentRelativeLayout.LayoutParams
            val info = params.percentLayoutInfo

            info.widthPercent = fav.infoWidthPercent

            name.requestLayout()

            itemView.setBackgroundResource(fav.itemBackgroundResId)

            if (fav.editFabShown)
                mEditFab.show()
            else {
                mEditFab.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                    override fun onHidden(fab: FloatingActionButton?) {
                        super.onHidden(fab)
                        mEditFab.visibility = View.INVISIBLE
                    }
                })
            }

            if (fav.deleteFabShown)
                mDeleteFab.show()
            else {
                mDeleteFab.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                    override fun onHidden(fab: FloatingActionButton?) {
                        super.onHidden(fab)
                        mDeleteFab.visibility = View.INVISIBLE
                    }
                })
            }

            if (fav.affordanceHandleVisible)
                mOrderingAffordanceHandle.visibility = View.VISIBLE
            else
                mOrderingAffordanceHandle.visibility = View.GONE

        }


        //TODO: should simply be forwarded to model which in turns reemits display data
        override fun onClick(v: View) {

            if (mSheetEditing && v.id != R.id.favorite_delete_fab && v.id != R.id.favorite_cancel_delete_fab)
                return

            when (v.id) {
                R.id.favorite_name -> if (!mEditing)
                    mItemClickListener.onFavoriteListItemClick(favoriteId!!)
                else
                //User pressed back to hide keyboard
                    showSoftInput()

                R.id.favorite_name_edit_fab -> {
                    mEditing = true
                    setupItemEditMode(true)
                    mItemClickListener.onFavoriteListItemNameEditBegin()
                }

                R.id.favorite_name_done_fab -> {
                    mEditing = false
                    setupItemEditMode(false)
                    mItemClickListener.onFavoriteListItemNameEditDone(favoriteId!!, name.text.toString())
                }

                R.id.favorite_delete_fab -> {

                    mDeleteFab.visibility = View.INVISIBLE
                    mOrderingAffordanceHandle.visibility = View.INVISIBLE
                    mDeleteCancelProgressFab.visibility = View.VISIBLE
                    mDeleteCancelCountDownTimer.start()
                }

                R.id.favorite_cancel_delete_fab -> {
                    mDeleteCancelCountDownTimer.cancel()
                    mDeleteCancelProgressFab.visibility = View.INVISIBLE
                    mDeleteFab.visibility = View.VISIBLE
                    mOrderingAffordanceHandle.visibility = View.VISIBLE
                    name.alpha = 1f
                }
            }
        }

        private fun setupItemEditMode(editing: Boolean) {
            if (editing) {
                mEditFab.hide()
                mDoneFab.show()

                name.isCursorVisible = true
                name.onFocusChangeListener = this
                name.setTextIsSelectable(true)
                name.isFocusableInTouchMode = true
                name.requestFocus()

                //API level 21+
                //nameText.setShowSoftInputOnFocus(true);

                showSoftInput()
            } else {
                hideSoftInput()

                name.isCursorVisible = false
                name.onFocusChangeListener = null
                name.setTextIsSelectable(false)
                name.isFocusableInTouchMode = false

                name.text = name.text.toString().trim { it <= ' ' }

                mDoneFab.hide()
                mEditFab.show()
            }
        }

        private fun showSoftInput() {
            mInputMethodManager.showSoftInput(name, InputMethodManager.SHOW_FORCED)
        }

        private fun hideSoftInput() {
            mInputMethodManager.hideSoftInputFromWindow(name.windowToken, 0)
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {

            val vTV = v as TextView

            if (hasFocus) {

                mNameBeforeEdit = vTV.text.toString()

            } else {

                if (mEditing) {
                    //Editing mode wasn't left from clicking done fab, restoring original name
                    vTV.text = mNameBeforeEdit

                    mEditing = false
                    setupItemEditMode(false)
                    mItemClickListener.onFavoristeListItemNameEditAbort()
                }
            }
        }

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {

                mItemStartDragListener.onFavoriteListItemStartDrag(this)
            }
            return false
        }

        override fun onItemSelected() {

            if (mSheetEditing)
                animateBackgroundColor(mResolvedThemeAccentTransparentColor, mResolvedThemeAccentColor, 250)
        }

        override fun onItemClear() {
            if (mSheetEditing)
                animateBackgroundColor(mResolvedThemeAccentColor, mResolvedThemeAccentTransparentColor, 250)
        }

        //http://stackoverflow.com/questions/2614545/animate-change-of-view-background-color-on-android/14467625#14467625
        private fun animateBackgroundColor(colorFrom: Int, colorTo: Int, durationMillisecond: Int) {
            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
            colorAnimation.duration = durationMillisecond.toLong()
            colorAnimation.addUpdateListener { valueAnimator -> itemView.setBackgroundColor(valueAnimator.animatedValue as Int) }
            colorAnimation.start()
        }
    }

    companion object {


        //following should be final
        private lateinit var mItemClickListener: OnFavoriteListItemClickListener
        //following should be final
        private lateinit var mItemStartDragListener: OnFavoriteListItemStartDragListener
        private lateinit var mInputMethodManager: InputMethodManager

        //Those variable to extract Context data at construction so that ViewHolders can statically reference them
        private var mResolvedThemeAccentColor: Int = 0
        private var mResolvedThemeAccentTransparentColor: Int = 0

        private var mSheetEditing = false

        private var mFavoriteItemDeleteCancelDurationMillis: Int = 0
    }
}
