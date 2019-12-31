package com.inlacou.inkslider

data class InkSliderMdl(
		/**
		 * All available colors. There can be more or less colors than values.
		 */
		var colors: List<Int>,
		/**
		 * All available values. There can be more or less values than colors.
		 */
		var values: List<Item>,
		/**
		 * Item currently selected
		 */
		var currentItem: Item = values.last(),
		/**
		 * Which side to display marker
		 */
		val displayMode: DisplayMode = DisplayMode.CENTER,
		/**
		 * How to display colors
		 */
		val colorMode: ColorMode = ColorMode.GRADIENT_CONTINUOUS,
		/**
		 * Corners on color bar edges
		 */
		val cornerRadius: Float = 100f,
		val reverse: Boolean = false,
		/**
		 * Called on value change (once, on button click or on press release)
		 */
		val onValueSet: ((item: Item, fromUser: Boolean) -> Unit)? = null,
		/**
		 * Called on value change (continuous while user moves the finger)
		 */
		val onValueChange: ((item: Item, fromUser: Boolean) -> Unit)? = null,
		/**
		 * Defines if the marker can stop in any place when user press released, or only on some hard points (the center of the realm associated with a value)
		 */
		val hardSteps: Boolean = true,
		/**
		 * If view is enabled or disabled
		 */
		var enabled: Boolean = true,
		/**
		 * If true, this will ignore the tries to set current value programatically when the user is touching the slider.
		 */
		var ignoreInputWhileUserInteraction: Boolean = false
){
	enum class DisplayMode {
		/**
		 * Text or icon
		 */
		LEFT_TOP,
		/**
		 * Text or icon
		 */
		RIGHT_BOTTOM,
		/**
		 * LEFT_TOP and RIGHT_BOTTOM
		 */
		BOTH_SIDES,
		/**
		 * Icon (not from values)
		 */
		CENTER,
		/**
		 * Needs display text color
		 */
		CENTER_SPECIAL,
		/**
		 * No display
		 */
		NONE }
	
	enum class ColorMode { NORMAL, GRADIENT, GRADIENT_CONTINUOUS }
	enum class Orientation { VERTICAL, HORIZONTAL }
	data class Item(val value: Any, val display: Display, val selectable: Boolean = true)
	data class Display(
			val string: String? = null,
			val arrowTintColor: Int? = null,
			val iconTintColor: Int? = null,
			val textColor: Int? = null,
			val icon: Int? = null)
	var disabled
		get() = !enabled
	    set(value) { enabled = !value }
}