package com.ludoscity.findmybikes.ui.sheet

import android.content.Context
import android.graphics.PointF
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView

/**
 * Created by F8Full on 2015-10-18.
 * LinearLayoutManager with altered smooth scrolling speed
 */
class ScrollingLinearLayoutManager(context: Context, orientation: Int, reverseLayout: Boolean, private val mDuration: Int) : LinearLayoutManager(context, orientation, reverseLayout) {

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State?, position: Int) {
        val firstVisibleChild = recyclerView.getChildAt(0)
        val itemHeight = firstVisibleChild.height
        val currentPosition = recyclerView.getChildLayoutPosition(firstVisibleChild)
        var distanceInPixels = Math.abs((currentPosition - position) * itemHeight)
        if (distanceInPixels == 0) {
            distanceInPixels = Math.abs(firstVisibleChild.y).toInt()
        }

        val smoothScroller = SmoothScroller(recyclerView.context, distanceInPixels, mDuration)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    private inner class SmoothScroller(context: Context, distanceInPixels: Int, duration: Int) : LinearSmoothScroller(context) {

        private val distanceInPixels: Float = distanceInPixels.toFloat()
        private val duration: Float = duration.toFloat()

        init {
            //float millisecondsPerPx = calculateSpeedPerPixel(context.getResources().getDisplayMetrics());
        }

        override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
            return this@ScrollingLinearLayoutManager
                    .computeScrollVectorForPosition(targetPosition)
        }

        override fun calculateTimeForScrolling(dx: Int): Int {
            val proportion = dx.toFloat() / distanceInPixels
            return (duration * proportion).toInt()
        }
    }
}
