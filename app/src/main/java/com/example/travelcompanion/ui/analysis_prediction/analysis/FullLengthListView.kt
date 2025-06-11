package com.kview

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

//https://stackoverflow.com/questions/24362717/listview-height-issue-inside-scrollview-in-android
class FullLengthListView : ListView {
    var isExpanded: Boolean = true

    constructor(
        context: Context?, attrs: AttributeSet?,
        defaultStyle: Int
    ) : super(context, attrs, defaultStyle)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isExpanded) {
            val expandSpec = MeasureSpec.makeMeasureSpec(
                MEASURED_SIZE_MASK,
                MeasureSpec.AT_MOST
            )
            super.onMeasure(widthMeasureSpec, expandSpec)

            val params = layoutParams
            params.height = measuredHeight
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}