package com.example.agsl

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * TODO: document your custom view class.
 */
class ShaderView : View {

    private var _exampleString: String? = null // TODO: use a default from R.string...
    private var _exampleColor: Int = Color.RED // TODO: use a default from R.color...
    private var _exampleDimension: Float = 0f // TODO: use a default from R.dimen...

    private lateinit var textPaint: TextPaint
    private var textWidth: Float = 0f
    private var textHeight: Float = 0f

    private val DURATION = 4000f
    private val COLOR_SHADER_SRC = """
       uniform float2 iResolution;
       uniform float iTime;
       uniform float iDuration;
       half4 main(in float2 fragCoord) {
          float2 scaled = abs(1.0-mod(fragCoord/iResolution.xy+iTime/(iDuration/2.0),2.0)); // 0~1
          return half4(scaled, 0, 1.0);
       }
    """

    private val shaderAnimator = ValueAnimator.ofFloat(0f, DURATION)
    private val runtimeShader = RuntimeShader(COLOR_SHADER_SRC)
    private lateinit var paint: Paint

    /**
     * The text to draw
     */
    var exampleString: String?
        get() = _exampleString
        set(value) {
            _exampleString = value
            invalidateTextPaintAndMeasurements()
        }

    /**
     * The font color
     */
    var exampleColor: Int
        get() = _exampleColor
        set(value) {
            _exampleColor = value
            invalidateTextPaintAndMeasurements()
        }

    /**
     * In the example view, this dimension is the font size.
     */
    var exampleDimension: Float
        get() = _exampleDimension
        set(value) {
            _exampleDimension = value
            invalidateTextPaintAndMeasurements()
        }

    /**
     * In the example view, this drawable is drawn above the text.
     */
    var exampleDrawable: Drawable? = null

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.ShaderView, defStyle, 0
        )

        _exampleString = a.getString(
            R.styleable.ShaderView_exampleString
        )
        _exampleColor = a.getColor(
            R.styleable.ShaderView_exampleColor,
            exampleColor
        )
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        _exampleDimension = a.getDimension(
            R.styleable.ShaderView_exampleDimension,
            exampleDimension
        )

        if (a.hasValue(R.styleable.ShaderView_exampleDrawable)) {
            exampleDrawable = a.getDrawable(
                R.styleable.ShaderView_exampleDrawable
            )
            exampleDrawable?.callback = this
        }

        a.recycle()

        // Set up a default TextPaint object
        textPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            textAlign = Paint.Align.LEFT
        }

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements()

        shaderAnimator.duration = DURATION.toLong()
        shaderAnimator.repeatCount = ValueAnimator.INFINITE
        shaderAnimator.repeatMode = ValueAnimator.RESTART
        shaderAnimator.interpolator = LinearInterpolator()

        runtimeShader.setFloatUniform("iDuration", DURATION )
        shaderAnimator.addUpdateListener { animation ->
            runtimeShader.setFloatUniform("iTime", animation.animatedValue as Float )
            invalidate() // redraw
        }
        shaderAnimator.start()

        paint = Paint().apply { shader = runtimeShader }
    }

    private fun invalidateTextPaintAndMeasurements() {
        textPaint.let {
            it.textSize = exampleDimension
            it.color = exampleColor
            textWidth = it.measureText(exampleString)
            textHeight = it.fontMetrics.bottom
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        exampleString?.let {
            // Draw the text.
            canvas.drawText(
                it,
                paddingLeft + (contentWidth - textWidth) / 2,
                paddingTop + (contentHeight + textHeight) / 2,
                textPaint
            )
        }

        // Draw the example drawable on top of the text.
        exampleDrawable?.let {
            it.setBounds(
                paddingLeft, paddingTop,
                paddingLeft + contentWidth, paddingTop + contentHeight
            )
            it.draw(canvas)
        }
    }

    override fun onDrawForeground(canvas: Canvas?) {
        canvas?.let {
            runtimeShader.setFloatUniform("iResolution", width.toFloat(), height.toFloat())
            canvas.drawPaint(paint)
        }
    }
}