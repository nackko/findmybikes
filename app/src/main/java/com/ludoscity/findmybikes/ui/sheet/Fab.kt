package com.ludoscity.findmybikes.ui.sheet

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gordonwong.materialsheetfab.AnimatedFab

/**
 * Created by F8Full on 2016-03-26.
 * from https://github.com/gowong/material-sheet-fab/blob/master/sample/src/main/java/com/gordonwong/materialsheetfab/sample/Fab.java
 */
class Fab : FloatingActionButton, AnimatedFab {
    //private static final int FAB_ANIM_DURATION = 200;

    var isShowRunning = false
        private set

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun show(translationX: Float, translationY: Float) {

        isShowRunning = true

        show(object : FloatingActionButton.OnVisibilityChangedListener() {
            override fun onShown(fab: FloatingActionButton?) {
                isShowRunning = false
                super.onShown(fab)
            }
        })
        // Set FAB's translation
        /*setTranslation(translationX, translationY);

        // Only use scale animation if FAB is hidden
        if (getVisibility() != View.VISIBLE) {
            // Pivots indicate where the animation begins from
            float pivotX = getPivotX() + translationX;
            float pivotY = getPivotY() + translationY;

            ScaleAnimation anim;
            // If pivots are 0, that means the FAB hasn't been drawn yet so just use the
            // center of the FAB
            if (pivotX == 0 || pivotY == 0) {
                anim = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
            } else {
                anim = new ScaleAnimation(0, 1, 0, 1, pivotX, pivotY);
            }

            // Animate FAB expanding
            anim.setDurationText(FAB_ANIM_DURATION);
            anim.setInterpolator(getInterpolator());
            startAnimation(anim);
        }
        setVisibility(View.VISIBLE);*/

    }

    /*private void setTranslation(float translationX, float translationY) {
        animate().setInterpolator(getInterpolator()).setDurationText(FAB_ANIM_DURATION)
                .translationX(translationX).translationY(translationY);
    }

    private Interpolator getInterpolator() {
        return AnimationUtils.loadInterpolator(getContext(), R.interpolator.msf_interpolator);
    }*/
}
