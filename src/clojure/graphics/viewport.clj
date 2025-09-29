(ns clojure.graphics.viewport
  (:require [clojure.core-ext :refer [clamp]]
            [com.badlogic.gdx.utils.viewport :as viewport]))

(defn update! [this width height {:keys [center?]}]
  (viewport/update! this width height :center? center?))

(defn unproject
  "Transforms the specified touch coordinates (`[x y]`) to world coordinates.
  The x- and y-coordinate of vec are assumed to be in touch coordinates (origin is the top left corner, y pointing down, x pointing to the right)

  Returns the world-coordinates as `[x y]`."
  [this [x y]]
  (viewport/unproject this
                      (clamp x
                             (:viewport/left-gutter-width this)
                             (:viewport/right-gutter-x    this))
                      (clamp y
                             (:viewport/top-gutter-height this)
                             (:viewport/top-gutter-y      this))))
