package com.thomy.library


import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRectF


class CircleImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_BORDER_WIDTH = 2
        private const val DEFAULT_BORDER_COLOR = Color.WHITE
        private const val DEFAULT_SIZE = 40


        private const val DEFAULT_INITIALS_TEXT = "??"

    }

    @Px
    var borderWidth: Float = dpToPx(DEFAULT_BORDER_WIDTH)

    @ColorInt
    private var borderColor: Int = DEFAULT_BORDER_COLOR
    private var initials: String = DEFAULT_INITIALS_TEXT

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val viewRect = Rect()
    private val borderRect = Rect()

    private var size = 0
    private var avatarMode = true

    init {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView)
            borderWidth = ta.getDimension(
                    R.styleable.CircleImageView_borderWidth, dpToPx(
                    DEFAULT_BORDER_WIDTH
            )
            )
            borderColor = ta.getColor(R.styleable.CircleImageView_borderColor, DEFAULT_BORDER_COLOR)
            initials = ta.getString(R.styleable.CircleImageView_initials) ?: DEFAULT_INITIALS_TEXT
            ta.recycle()
        }

        borderPaint.apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        }

        if (drawable == null) avatarMode = false

        scaleType = ScaleType.CENTER_CROP
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val initSize = resolveDefaultSize(widthMeasureSpec.coerceAtMost(heightMeasureSpec))
        setMeasuredDimension(initSize.coerceAtLeast(size), initSize.coerceAtLeast(size))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w == 0) return

        viewRect.apply {
            left = 0
            top = 0
            right = w
            bottom = h
        }

        prepareShader(w, h)
    }


    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(super.onSaveInstanceState())
                .also { state ->
                    state.avatarMode = avatarMode
                    state.borderWidth = borderWidth
                    state.borderColor = borderColor
                }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state)

            avatarMode = state.avatarMode
            borderWidth = state.borderWidth
            borderColor = state.borderColor

            borderPaint.apply {
                strokeWidth = borderWidth
                color = borderColor
            }

        } else super.onRestoreInstanceState(state)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)

        if (avatarMode) prepareShader(width, height)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)

        if (avatarMode) prepareShader(width, height)
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)

        if (avatarMode) prepareShader(width, height)
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        prepareShader(width, height)
    }


    fun setBorderWidth(@Dimension width: Int) {
        borderWidth = dpToPx(width)
        borderPaint.strokeWidth = borderWidth

        invalidate()
    }


    private fun prepareShader(w: Int, h: Int) {
        if (w == 0 || drawable == null) return

        val srcW = drawable.intrinsicWidth.toFloat()
        val srcH = drawable.intrinsicHeight.toFloat()

        val wFactor = if (srcW > srcH) srcW / srcH else 1f
        val hFactor = if (srcH > srcW) srcH / srcW else 1f
        val dX = (w * (wFactor - 1)).toInt()
        val dY = (h * (hFactor - 1)).toInt()

        val tmpBm = drawable.toBitmap(w + dX, h + dY, Bitmap.Config.ARGB_8888)
        val srcBm = Bitmap.createBitmap(tmpBm, dX / 2, dY / 2, w, h)

        avatarPaint.shader = BitmapShader(srcBm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    private fun resolveDefaultSize(spec: Int): Int =
            when (MeasureSpec.getMode(spec)) {
                MeasureSpec.UNSPECIFIED -> dpToPx(DEFAULT_SIZE).toInt()
                MeasureSpec.AT_MOST -> MeasureSpec.getSize(spec)
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(spec)
                else -> MeasureSpec.getSize(spec)
            }

    private fun drawAvatar(canvas: Canvas) {
        canvas.drawOval(viewRect.toRectF(), avatarPaint)
    }


    override fun onDraw(canvas: Canvas) {
        if (drawable != null && avatarMode) {
            drawAvatar(canvas)

        }

        val half = (borderWidth / 2).toInt()

        borderRect.set(viewRect)
        borderRect.inset(half, half)

        canvas.drawOval(borderRect.toRectF(), borderPaint)
    }


    private class SavedState : BaseSavedState, Parcelable {
        var avatarMode: Boolean = true
        var borderWidth: Float = 0f
        var borderColor: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            parcel.apply {
                avatarMode = readInt() == 1
                borderWidth = readFloat()
                borderColor = readInt()
            }
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)

            parcel.apply {
                writeInt(if (avatarMode) 1 else 0)
                writeFloat(borderWidth)
                writeInt(borderColor)
            }
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel) = SavedState(parcel)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }

    }

    private fun dpToPx(dp: Int): Float = dp.toFloat() * context.resources.displayMetrics.density
}
