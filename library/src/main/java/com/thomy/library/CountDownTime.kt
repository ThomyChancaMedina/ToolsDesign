package com.thomy.library


import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.thomy.library.R

data class CountDownTimeAttributeData(
    val timeTextSize: Float? = null,
    val timeTextColor: Int? = null,
    val descriptionText: String? = null,
    val descriptionTextColor: Int? = null,
    val descriptionTextSize: Float? = null,
    val innerCircleColor: Int? = null,
    val outerCircleColor: Int? = null,
    val clockwise: Boolean? = null,
    val animation: Boolean? = null
)

private fun readAttributes(context: Context, attrs: AttributeSet?): CountDownTimeAttributeData {
    attrs ?: return CountDownTimeAttributeData()
    val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.CountDownTime, 0, 0)
    val timeTextSize: Float?
    val timeTextColor: Int?
    val descriptionText: String?
    val descriptionTextColor: Int?
    val descriptionTextSize: Float?
    val innerCircleColor: Int?
    val outerCircleColor: Int?
    val clockwise: Boolean?
    val animation: Boolean?
    return try {
        attributes.run {
            timeTextSize =
                getDimension(
                    R.styleable.CountDownTime_timeTextSize,
                    resources.getDimension(R.dimen.font_xxlarge)
                )
            timeTextColor =
                getResourceId(R.styleable.CountDownTime_timeTextColor, R.color.colorPrimary)
            descriptionText = getString(R.styleable.CountDownTime_descriptionText)
            descriptionTextColor =
                getResourceId(R.styleable.CountDownTime_descriptionTextColor, R.color.greyDark)
            descriptionTextSize =
                getDimension(
                    R.styleable.CountDownTime_descriptionTextSize,
                    resources.getDimension(R.dimen.font_large)
                )
            innerCircleColor =
                getResourceId(R.styleable.CountDownTime_innerCircleColor, R.color.grey)
            outerCircleColor =
                getResourceId(R.styleable.CountDownTime_outerCircleColor, R.color.colorPrimary)
            clockwise = getBoolean(R.styleable.CountDownTime_clockwise, false)
            animation = getBoolean(R.styleable.CountDownTime_animation, true)
        }

        return CountDownTimeAttributeData(
            timeTextSize,
            timeTextColor,
            descriptionText,
            descriptionTextColor,
            descriptionTextSize,
            innerCircleColor,
            outerCircleColor,
            clockwise,
            animation
        )
    } catch (e: Exception) {
        CountDownTimeAttributeData()
    } finally {
        attributes.recycle()
    }
}

class CountDownTime @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0

) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var progressBarCircle: ProgressBar
    private lateinit var textNumViewTime: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var timer: CountDownTimer
    private var timerCount: Long = 0L
    private var remainingTimeSecond: Long = 0L
    private lateinit var animation: ObjectAnimator
    private var animationAllowed = false
    private var runClockWise = false
    private var _onCountDownTimerStarted: (() -> Unit)? = null
    private var _onCountDownTimerStopped: (() -> Unit)? = null
    private var _onCountDownTimerRunning: ((remainingTime: Long) -> Unit)? = null

    init {
        initViews()
        setAttributes(context, attrs)
    }

    private fun initViews() {
        LayoutInflater.from(context).inflate(R.layout.count_down_timer_view, this, true)
        progressBarCircle = this.findViewById(R.id.progressBarCircle)
        textNumViewTime = this.findViewById(R.id.txtProgress)
        textViewDescription = this.findViewById(R.id.textViewDescription)
    }

    /*
     * establecer atributos para ver datos
     */
    private fun setAttributes(context: Context, attrs: AttributeSet?) {
        val data = readAttributes(context, attrs)

        data.apply {
            textViewDescription.apply {
                text = descriptionText
                setTextColor(ContextCompat.getColor(context, descriptionTextColor!!))
                textSize = descriptionTextSize!! / resources.displayMetrics.scaledDensity
            }

            textNumViewTime.apply {
                textSize = timeTextSize!! / resources.displayMetrics.scaledDensity
                setTextColor(ContextCompat.getColor(context, timeTextColor!!))
            }

            progressBarCircle.apply {
                background.setColorFilter(
                    ContextCompat.getColor(context, innerCircleColor!!),
                    PorterDuff.Mode.SRC_IN
                )

                progressDrawable.setColorFilter(
                    ContextCompat.getColor(context, outerCircleColor!!),
                    PorterDuff.Mode.SRC_IN
                )
            }

            if (clockwise!!) {
                rotateToClockWise()
            }
            animationAllowed = animation!!
        }
    }

    /*
    * Establecer valores iniciales de la barra de progreso
    */
    private fun setProgressBarValues() {
        progressBarCircle.apply {
            max = timerCount.toInt() * 1000
            progress = timerCount.toInt() * 1000
        }
    }

    /*
     * Establecer la animación en la barra de progreso
     * @param pb: progress bar which will be added animation
     * @param progressTo : duration time that animation will continue
     */
    private fun setProgressAnimate(pb: ProgressBar, progressTo: Int) {
        animation = ObjectAnimator.ofInt(pb, "progress", pb.progress, 0)
        animation.apply {
            setAutoCancel(true)
            duration = progressTo.toLong()
            interpolator = LinearInterpolator()
            start()
        }
    }

    /*
     * Cambiar la dirección de trabajo de la barra de progreso
     */
    private fun rotateToClockWise() {
        progressBarCircle.apply {
            max = timerCount.toInt() * 1000
            progress = timerCount.toInt() * 1000
        }
    }

    /*
     * Establecer el contador de inicio del temporizador e iniciar el temporizador de cuenta atrás.
     * @param timerCount: timer length value
     * @param animation: true -> progress run with animation
     *                  false -> progress run standard
     * @param runClockwise: progress running direction
     */
    fun startTimer(
        timerCount: Long,
        animation: Boolean = animationAllowed,
        runClockwise: Boolean = runClockWise,
        onCountDownTimerStarted: (() -> Unit)? = null,
        onCountDownTimerStopped: (() -> Unit)? = null,
        onCountDownTimerRunning: ((remainingTime: Long) -> Unit)? = null
    ) {
        this.timerCount = timerCount
        this.animationAllowed = animation
        this._onCountDownTimerStarted = onCountDownTimerStarted
        this._onCountDownTimerStopped = onCountDownTimerStopped
        this._onCountDownTimerRunning = onCountDownTimerRunning
        if (runClockwise) {
            rotateToClockWise()
        }
        setProgressBarValues()
        _onCountDownTimerStarted?.invoke()
        timer = object : CountDownTimer(timerCount * 1000, 1000) {
            override fun onFinish() {
                textNumViewTime.text = "0"
                setProgressBarValues()
                _onCountDownTimerStopped?.invoke()
                stopTimer()
            }

            override fun onTick(millisUntilFinished: Long) {
                remainingTimeSecond = millisUntilFinished / 1000
                textNumViewTime.text = (remainingTimeSecond).toString()
                _onCountDownTimerRunning?.invoke(remainingTimeSecond)
                if (animationAllowed) {
                    setProgressAnimate(progressBarCircle, millisUntilFinished.toInt())
                } else {
                    progressBarCircle.progress = (millisUntilFinished).toInt()
                }
            }
        }.start()
    }

    /*
     * Detener el temporizador de cuenta regresiva
     */
    fun stopTimer() {
        timer.cancel()
    }

    /*
     * Detener y reiniciar el temporizador de cuenta atrás
     */
    fun resetTimer() {
        stopTimer()
        if (animationAllowed) {
            animation.cancel()
        }
        startTimer(
            timerCount,
            animationAllowed,
            runClockWise,
            _onCountDownTimerStarted,
            _onCountDownTimerStopped,
            _onCountDownTimerRunning
        )
    }

    /*
     * To set the description text that under the timer view
     */
    fun setDescriptionText(descriptionText: String?) {
        textViewDescription.text = descriptionText
    }

    /*
     * To change the description text color
     */
    fun setDescriptionTextColor(descriptionTextColor: Int) {
        textViewDescription.setTextColor(descriptionTextColor)
    }

    /*
     * To change the description text size
     */
    fun setDescriptionTextSize(descriptionTextSize: Float) {
        textViewDescription.textSize = descriptionTextSize / resources.displayMetrics.scaledDensity
    }

    /*
     * To change the remaining time text size
     */
    fun setTimeTextSize(timeTextSize: Float) {
        textNumViewTime.textSize = timeTextSize / resources.displayMetrics.scaledDensity
    }

    /*
     * To change the remaining time text color
     */
    fun setTimeTextColor(timeTextColor: Int) {
        textNumViewTime.setTextColor(timeTextColor)
    }

    /*
     * To change the color of the progress background
     */
    fun setProgressInnerCircleColor(innerCircleColor: Int) {
        progressBarCircle.background.setColorFilter(
            ContextCompat.getColor(context, innerCircleColor),
            PorterDuff.Mode.SRC_IN
        )
    }

    /*
     * To change the color of the progress drawable
     */
    fun setProgressOuterCircleColor(outerCircleColor: Int) {
        progressBarCircle.progressDrawable.setColorFilter(
            ContextCompat.getColor(context, outerCircleColor),
            PorterDuff.Mode.SRC_IN
        )
    }

    override fun onDetachedFromWindow() {
        stopTimer()
        super.onDetachedFromWindow()
    }
}