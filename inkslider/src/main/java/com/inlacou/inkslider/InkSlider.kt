package com.inlacou.inkslider

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.widget.ImageViewCompat
import com.inlacou.pripple.RippleLinearLayout
import kotlin.math.roundToInt

open class InkSlider @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
	: FrameLayout(context, attrs, defStyleAttr) {
	
	private var currentHeight: Float? = null
	private var plus: View? = null
	private var minus: View? = null
	private var surfaceLayout: RelativeLayout? = null
	private var linearLayoutColors: LinearLayout? = null
	private var linearLayoutSliders: LinearLayout? = null
	private var linearLayoutDisplayLeft: LinearLayout? = null
	private var linearLayoutDisplayRight: LinearLayout? = null
	private var tvDisplayLeft: TextView? = null
	private var tvDisplayRight: TextView? = null
	private var ivDisplayLeft: ImageView? = null
	private var ivDisplayRight: ImageView? = null
	private var ivDisplayLeftArrow: ImageView? = null
	private var ivDisplayRightArrow: ImageView? = null
	private var rippleLayoutTop: RippleLinearLayout? = null
	private var rippleLayoutBottom: RippleLinearLayout? = null
	
	private fun bindViews() {
		plus = findViewById(R.id.plus)
		minus = findViewById(R.id.minus)
		surfaceLayout = findViewById(R.id.view_base_layout_surface)
		linearLayoutColors = findViewById(R.id.linearLayout_colors)
		linearLayoutSliders = findViewById(R.id.linearLayout_slider)
		linearLayoutDisplayLeft = findViewById(R.id.linearLayout_display_left)
		linearLayoutDisplayRight = findViewById(R.id.linearLayout_display_right)
		tvDisplayLeft = findViewById(R.id.tv_display_left)
		tvDisplayRight = findViewById(R.id.tv_display_right)
		ivDisplayLeft = findViewById(R.id.iv_display_left)
		ivDisplayRight = findViewById(R.id.iv_display_right)
		ivDisplayLeftArrow = findViewById(R.id.iv_display_left_arrow)
		ivDisplayRightArrow = findViewById(R.id.iv_display_right_arrow)
		rippleLayoutTop = findViewById(R.id.ripple_layout_top)
		rippleLayoutBottom = findViewById(R.id.ripple_layout_bottom)
	}
	
	var model: InkSliderMdl = InkSliderMdl(colors = listOf(), values = listOf(InkSliderMdl.Item(value = "Any", display = InkSliderMdl.Display("Any"), selectable = true)))
		set(value) {
			field = value
			controller.model = value
			populate()
		}
	private lateinit var controller: InkSliderCtrl
	val items get() = model.values
	var currentItem: InkSliderMdl.Item
		get() = model.currentItem
		private set(value) {
			if(model.currentItem!=value){
				model.currentItem = value
				forceUpdate()
				controller.onCurrentItemChanged(value, false)
			}
		}
	
	/* Virtual variables for cleaner code */
	private val realHeight get() = linearLayoutColors?.height ?: height
	private val stepSize get() = realHeight/(model.values.size)
	private val topSpacing get() = linearLayoutColors?.getCoordinates()?.top ?: 0
	
	init {
		this.initialize()
		setListeners()
		populate()
	}
	
	protected fun initialize() {
		val rootView = View.inflate(context, R.layout.view_ink_slider_ripple, this)
		initialize(rootView)
	}
	
	protected fun initialize(view: View) {
		controller = InkSliderCtrl(view = this, model = model)
		bindViews()
	}
	
	protected fun populate() {
		clearItems()
		addItems()
		clearDisplays()
	}
	
	/* Public methods */
	/**
	 * Makes view appear with an android.R.anim.fade_in animation
	 */
	fun inAnimation() {
		val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
		linearLayoutColors?.startAnimation(animation)
	}
	
	fun setCurrentItemByIndex(volume: Int, fireListener: Boolean) {
		if(currentItem.display!=items[volume].display) {
			model.currentItem = items[volume]
			forceUpdate()
			model.onValueSet?.invoke(model.currentItem, fireListener)
		}
	}
	
	fun setCurrentItem(item: InkSliderMdl.Item, fireListener: Boolean) {
		if(currentItem.display!=item.display) {
			model.currentItem = item
			forceUpdate()
			model.onValueSet?.invoke(model.currentItem, fireListener)
		}
	}
	
	/**
	 * Returns whether the component is enabled or not
	 */
	override fun isEnabled(): Boolean = model.enabled
	
	/**
	 * Changes the component enabled state and updates it accordingly
	 */
	override fun setEnabled(enabled: Boolean) {
		val changed = enabled!=model.enabled
		model.enabled = enabled
		if(changed) if(enabled) onEnabled() else onDisabled()
	}
	/* /Public methods */
	
	/**
	 * Clears both displays resetting position and making them View.INVISIBLE
	 */
	private fun clearDisplays(){
		linearLayoutDisplayLeft?.setMargins(top = 0)
		linearLayoutDisplayRight?.setMargins(top = 0)
		linearLayoutDisplayLeft?.setVisible(visible = false, holdSpaceOnDissapear = true)
		linearLayoutDisplayRight?.setVisible(visible = false, holdSpaceOnDissapear = true)
	}
	
	/**
	 * Removes all views from linearLayout
	 */
	private fun clearItems() {
		linearLayoutColors?.removeAllViews()
	}
	
	/**
	 * Adds an item to the linearLayout for each color in model colors array
	 */
	private fun addItems() {
		model.colors.forEach{ addIcon(it) }
	}
	
	/**
	 * Adds an icon to the linearLayout with provided {@param colorResId} background color
	 */
	private fun addIcon(colorResId: Int) {
		val view = View(context)
		linearLayoutColors?.addView(view)
		view.apply {
			setBackgroundColor(colorResId)
			layoutParams = (layoutParams).apply { height = resources.getDimension(R.dimen.inkslider_row_height).toInt() }
		}
	}
	
	private var longClickLastTrigger: Long = 0
	private var longClickSpeedMax = 500
	private var longClickSpeedMin = 50
	private var longClickSpeedStep = 150
	private var longClickSpeed = longClickSpeedMax
	
	@SuppressLint("ClickableViewAccessibility")
	private fun setListeners() {
		//Touch listeners
		rippleLayoutTop?.isClickable = true
		rippleLayoutTop?.setOnTouchListener { view, motionEvent ->
			when(motionEvent?.action){
				android.view.MotionEvent.ACTION_DOWN -> {
					//Start
					attemptClaimDrag()
					controller.onPlusClick()
					longClickLastTrigger = System.currentTimeMillis()
					longClickSpeed = longClickSpeedMax
				}
				android.view.MotionEvent.ACTION_MOVE -> {
					rippleLayoutTop?.onTouchEvent(motionEvent)
					val now = System.currentTimeMillis()
					if(now-longClickLastTrigger>longClickSpeed) {
						longClickLastTrigger = now
						if(longClickSpeed>longClickSpeedMin) longClickSpeed -= longClickSpeedStep
						controller.onPlusClick()
					}
				}
			}
			false
		}
		
		rippleLayoutBottom?.isClickable = true
		rippleLayoutBottom?.setOnTouchListener { view, motionEvent ->
			when(motionEvent?.action){
				android.view.MotionEvent.ACTION_DOWN -> {
					//Start
					attemptClaimDrag()
					controller.onMinusClick()
					longClickLastTrigger = System.currentTimeMillis()
					longClickSpeed = longClickSpeedMax
				}
				android.view.MotionEvent.ACTION_MOVE -> {
					val now = System.currentTimeMillis()
					if(now-longClickLastTrigger>longClickSpeed) {
						longClickLastTrigger = now
						if(longClickSpeed>longClickSpeedMin) longClickSpeed -= longClickSpeedStep
						controller.onMinusClick()
					}
				}
			}
			false
		}
		
		//Touch listener
		linearLayoutColors?.setOnTouchListener { _, event ->
			val relativePosition = event.y //reaches 0 at top linearLayoutColors and goes on the minus realm if you keep going up
			currentHeight = relativePosition
			val roughStep = (relativePosition/stepSize)
			val step = (relativePosition/stepSize).roundToInt()
			val newItem = when {
				step<=0 -> model.values.getFirstSelectable()
				step>=model.values.size -> model.values.getLastSelectable()
				else -> model.values.getNearestSelectable(roughIndex = roughStep)
			}
			if(currentItem!=newItem) {
				controller.onCurrentItemChanged(newItem, true)
			}
			model.currentItem = newItem
			updateDisplays()
			
			when(event.action){
				android.view.MotionEvent.ACTION_DOWN -> {
					attemptClaimDrag()
					true
				}
				android.view.MotionEvent.ACTION_CANCEL -> false
				android.view.MotionEvent.ACTION_UP -> {
					if(model.hardSteps) forceUpdate() else controller.onTouchRelease()
					false
				}
				android.view.MotionEvent.ACTION_MOVE -> true
				else -> false
			}
		}
	}
	
	internal fun forceUpdate() {
		val currentItemPos = model.values.indexOf(model.currentItem)
		currentHeight = (stepSize*(currentItemPos)).toFloat()
		updateDisplays()
		controller.onValueSet(false)
	}
	
	private fun updateDisplays() {
		//Shows/hides displays depending on current DisplayMode
		linearLayoutDisplayLeft?.setVisible(model.displayMode==InkSliderMdl.DisplayMode.LEFT || model.displayMode==InkSliderMdl.DisplayMode.BOTH, true)
		linearLayoutDisplayRight?.setVisible(model.displayMode==InkSliderMdl.DisplayMode.RIGHT || model.displayMode==InkSliderMdl.DisplayMode.BOTH, true)
		
		//Style display
		model.currentItem.display.let { display ->
			//Set display text
			display.string.let {
				if (it != null) {
					tvDisplayRight?.text = it
					tvDisplayLeft?.text = it
				}
				tvDisplayRight?.setVisible(it!=null)
				tvDisplayLeft?.setVisible(it!=null)
			}
			//Set display text color
			display.textColor?.let {
				tvDisplayRight?.setTextColor(it)
				tvDisplayLeft?.setTextColor(it)
			}
			//Set display icon
			display.icon.let {
				if (it != null) {
					ivDisplayLeft?.setDrawableRes(it)
					ivDisplayRight?.setDrawableRes(it)
				}
				ivDisplayLeft?.setVisible(it!=null)
				ivDisplayRight?.setVisible(it!=null)
			}
			//Set display icon color
			display.iconTintColor?.let {
				ivDisplayLeft?.tint(it)
				ivDisplayRight?.tint(it)
			}
			//Set display icon color
			display.arrowTintColor?.let {
				ivDisplayLeftArrow?.tint(it)
				ivDisplayRightArrow?.tint(it)
			}
		}
		
		//Set display position
		currentHeight?.toInt()?.let {
			if (it in 1 until (linearLayoutColors?.height ?: height)) {
				linearLayoutDisplayLeft?.setMargins(top = it-((linearLayoutDisplayLeft?.height?:0)/2)+topSpacing)
				linearLayoutDisplayRight?.setMargins(top = it-((linearLayoutDisplayLeft?.height?:0)/2)+topSpacing)
			}
		}
	}
	
	/**
	 * Tries to claim the user's drag motion, and requests disallowing any
	 * ancestors from stealing events in the drag.
	 */
	private fun attemptClaimDrag() {
		parent?.requestDisallowInterceptTouchEvent(true)
	}
	
	/**
	 * Changes component to it's enabled (expanded) state
	 */
	private fun onEnabled() {
		linearLayoutColors?.onDrawn { forceUpdate() }
		addItems()
	}
	
	/**
	 * Changes component to it's disabled (collapsed) state
	 */
	private fun onDisabled() {
		clearItems()
		clearDisplays()
	}
	
}

