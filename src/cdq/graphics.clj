(ns cdq.graphics
  (:require [clojure.gdx.utils.viewport :as viewport]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.math.utils :refer [clamp]]
            [clojure.gdx.input :as input]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.utils :as utils]))

(defn set-cursor [{:keys [cdq.graphics/cursors]} cursor-key]
  (clojure.gdx.graphics/set-cursor (utils/safe-get cursors cursor-key)))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [viewport]
  (let [mouse-x (clamp (input/x)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (input/y)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (viewport/unproject viewport mouse-x mouse-y)))

(defn mouse-position [ui-viewport]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position ui-viewport)))

(defn world-mouse-position [world-viewport]
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))

(defn pixels->world-units [{:keys [cdq.graphics/world-unit-scale]} pixels]
  (* (int pixels) world-unit-scale))
