package com.goodwy.gallery.views

import android.content.Context
import android.graphics.*
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.goodwy.commons.extensions.getProperPrimaryColor
import com.goodwy.gallery.R
import com.goodwy.gallery.interfaces.CanvasListener
import com.goodwy.gallery.models.CanvasOp
import com.goodwy.gallery.models.MyParcelable
import com.goodwy.gallery.models.MyPath
import com.goodwy.gallery.models.PaintOptions

class EditorDrawCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mColor = 0
    private var mWasMultitouch = false

    private var mPaths = LinkedHashMap<Path, PaintOptions>()
    private var mPaint = Paint()
    private var mPath = MyPath()
    private var mPaintOptions = PaintOptions()

    private var backgroundBitmap: Bitmap? = null

    private val MIN_ERASER_WIDTH = 20f
    private val MAX_HISTORY_COUNT = 1000
    private val BITMAP_MAX_HISTORY_COUNT = 60
    var mListener: CanvasListener? = null
    var mBackgroundBitmap: Bitmap? = null
    private var mOperations = ArrayList<CanvasOp>()
    private var mUndoneOperations = ArrayList<CanvasOp>()
    private var mLastOperations = ArrayList<CanvasOp>()
    private var mLastBackgroundBitmap: Bitmap? = null
    private var mIsEraserOn = false
    private var mBackgroundColor = 0

    init {
        mColor = context.getProperPrimaryColor()
        mPaint.apply {
            color = mPaintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 40f
            isAntiAlias = true
        }
        updateUndoVisibility()
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = MyParcelable(superState!!)
        savedState.operations = mOperations
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is MyParcelable) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        mOperations = state.operations
        updateUndoVisibility()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)
        }

        if (mOperations.isNotEmpty()) {
            val bitmapOps = mOperations.filterIsInstance<CanvasOp.BitmapOp>()
            val bitmapOp = bitmapOps.lastOrNull()
            if (bitmapOp != null) {
                canvas.drawBitmap(bitmapOp.bitmap, 0f, 0f, null)
            }

            // only perform path ops after last bitmap op as any previous path operations are already visible due to the bitmap op
            val startIndex = if (bitmapOp != null) mOperations.indexOf(bitmapOp) else 0
            val endIndex = mOperations.lastIndex
            val pathOps = mOperations.slice(startIndex..endIndex).filterIsInstance<CanvasOp.PathOp>()
            for (pathOp in pathOps) {
                changePaint(pathOp.paintOptions)
                canvas.drawPath(pathOp.path, mPaint)
            }
        }

        /*for ((key, value) in mPaths) {
            changePaint(value)
            canvas.drawPath(key, mPaint)
        }*/

        changePaint(mPaintOptions)
        canvas.drawPath(mPath, mPaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x //val x = event.rawX
        val y = event.y //val y = event.rawY

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mWasMultitouch = false
                mStartX = x
                mStartY = y
                actionDown(x, y)
                mUndoneOperations.clear()
                updateRedoVisibility(false)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !mWasMultitouch) {
                    actionMove(x, y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> actionUp()
            MotionEvent.ACTION_POINTER_DOWN -> mWasMultitouch = true
        }

        invalidate()
        return true
    }

    private fun actionDown(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mCurX = x
        mCurY = y
    }

    private fun actionMove(x: Float, y: Float) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        if (!mWasMultitouch) {
            mPath.lineTo(mCurX, mCurY)

            // draw a dot on click
            if (mStartX == mCurX && mStartY == mCurY) {
                mPath.lineTo(mCurX, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY)
            }
        }

        addOperation(CanvasOp.PathOp(mPath, mPaintOptions))

        updateUndoVisibility()
        //mPaths[mPath] = mPaintOptions
        mPath = MyPath()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth, mPaintOptions.isEraser)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = if (paintOptions.isEraser) mBackgroundColor else paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
        if (paintOptions.isEraser && mPaint.strokeWidth < MIN_ERASER_WIDTH) {
            mPaint.strokeWidth = MIN_ERASER_WIDTH
        }
    }

    fun updateColor(newColor: Int) {
        mPaintOptions.color = newColor
    }

    fun updateBrushSize(newBrushSize: Int) {
        mPaintOptions.strokeWidth = resources.getDimension(R.dimen.full_brush_size) * (newBrushSize / 100f)
    }

    fun updateBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        return bitmap
    }

    fun undoOld() {
        if (mPaths.isEmpty()) {
            return
        }

        val lastKey = mPaths.keys.lastOrNull()
        mPaths.remove(lastKey)
        invalidate()
    }

    fun undo() {
        if (mOperations.isEmpty() && mLastOperations.isNotEmpty()) {
            mOperations = mLastOperations.clone() as ArrayList<CanvasOp>
            mBackgroundBitmap = mLastBackgroundBitmap
            mLastOperations.clear()
            updateUndoVisibility()
            invalidate()
            return
        }

        if (mOperations.isNotEmpty()) {
            val lastOp = mOperations.removeLast()
            mUndoneOperations.add(lastOp)
            invalidate()
        }
        updateUndoRedoVisibility()
    }

    fun redo() {
        if (mUndoneOperations.isNotEmpty()) {
            val undoneOperation = mUndoneOperations.removeLast()
            addOperation(undoneOperation)
            invalidate()
        }
        updateUndoRedoVisibility()
    }

    fun toggleEraser(isEraserOn: Boolean) {
        mIsEraserOn = isEraserOn
        mPaintOptions.isEraser = isEraserOn
        invalidate()
    }

    private fun addOperation(operation: CanvasOp) {
        mOperations.add(operation)

        // maybe free up some memory
        while (mOperations.size > MAX_HISTORY_COUNT) {
            val item = mOperations.removeFirst()
            if (item is CanvasOp.BitmapOp) {
                item.bitmap.recycle()
            }
        }

        val ops = mOperations.filterIsInstance<CanvasOp.BitmapOp>()
        if (ops.size > BITMAP_MAX_HISTORY_COUNT) {
            val start = ops.lastIndex - BITMAP_MAX_HISTORY_COUNT
            val bitmapOp = ops.slice(start..ops.lastIndex).first()

            val startIndex = mOperations.indexOf(bitmapOp)
            mOperations = mOperations.slice(startIndex..mOperations.lastIndex) as ArrayList<CanvasOp>
        }
    }

    private fun updateUndoRedoVisibility() {
        updateUndoVisibility()
        updateRedoVisibility()
    }

    private fun updateUndoVisibility() {
        mListener?.toggleUndoVisibility(mOperations.isNotEmpty() || mLastOperations.isNotEmpty())
    }

    private fun updateRedoVisibility(visible: Boolean = mUndoneOperations.isNotEmpty()) {
        mListener?.toggleRedoVisibility(visible)
    }

    fun clearCanvas() {
        mLastOperations = mOperations.clone() as ArrayList<CanvasOp>
        mLastBackgroundBitmap = mBackgroundBitmap
        mBackgroundBitmap = null
        mPath.reset()
        mOperations.clear()
        updateUndoVisibility()
        invalidate()
    }

    fun setColor(newColor: Int) {
        mPaintOptions.color = newColor
    }

    fun updateBackgroundColor(newColor: Int) {
        mBackgroundColor = newColor
        setBackgroundColor(newColor)
        mBackgroundBitmap = null
    }
}
