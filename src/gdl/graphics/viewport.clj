(ns gdl.graphics.viewport
  (:require [gdl.math :refer [clamp]]))

(defprotocol Viewport
  (update! [_ width height {:keys [center?]}])
  (unproject [_ x y]))

(defn unproject-clamp
  [this [x y]]
  (unproject this
             (clamp x
                    (:viewport/left-gutter-width this)
                    (:viewport/right-gutter-x    this))
             (clamp y
                    (:viewport/top-gutter-height this)
                    (:viewport/top-gutter-y      this))))
