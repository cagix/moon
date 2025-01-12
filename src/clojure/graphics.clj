(ns clojure.graphics)

(defn new-cursor [pixmap hotspot-x hotspot-y]
  (.newCursor com.badlogic.gdx.Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor com.badlogic.gdx.Gdx/graphics cursor))


(defn delta-time
  "The time span between the current frame and the last frame in seconds."
  []
  (.getDeltaTime com.badlogic.gdx.Gdx/graphics))

(defn frames-per-second
  "The average number of frames per second."
  []
  (.getFramesPerSecond com.badlogic.gdx.Gdx/graphics))

(defn def-color
  "A general purpose class containing named colors that can be changed at will. For example, the markup language defined by the BitmapFontCache class uses this class to retrieve colors and the user can define his own colors.

  Convenience method to add a color with its name. The invocation of this method is equivalent to the expression Colors.getColors().put(name, color)

  Parameters:
  name - the name of the color
  color - the color
  Returns:
  the previous color associated with name, or null if there was no mapping for name ."
  [name-str color]
  (com.badlogic.gdx.graphics.Colors/put name-str color))
