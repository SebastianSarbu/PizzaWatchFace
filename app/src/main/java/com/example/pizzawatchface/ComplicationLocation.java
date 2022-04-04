package com.example.pizzawatchface;

/**
 * Used by associated watch face ({@link com.example.pizzawatchface.PizzaWatchFaceService}) to let this
 * configuration Activity know which complication locations are supported, their ids, and
 * supported complication data types.
 */
public enum ComplicationLocation {
    RIGHT,
    TOP_RIGHT,
    TOP,
    TOP_LEFT,
    LEFT,
    BOTTOM,
    CENTER,
    TOP_RIGHT_RANGED,
    BOTTOM_RIGHT_RANGED,
    BOTTOM_LEFT_RANGED,
    TOP_LEFT_RANGED
}