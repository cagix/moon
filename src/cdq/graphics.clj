(ns cdq.graphics
  (:require [cdq.gdx.utils.viewport :as viewport]
            [clojure.gdx.graphics :as graphics]
            [cdq.graphics.2d.batch :as batch]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.graphics.camera :as camera]
            [cdq.math.utils :refer [clamp]]
            [clojure.gdx.input :as input]
            [cdq.gdx.utils.viewport :as viewport]
            [cdq.graphics.color :as color]
            [cdq.utils :as utils]))

(defn set-cursor [{:keys [cdq.graphics/cursors]} cursor-key]
  (clojure.gdx.graphics/set-cursor (utils/safe-get cursors cursor-key)))

(defn- draw-with [{:keys [cdq.graphics/batch
                          cdq.graphics/shape-drawer] :as c}
                 viewport
                 unit-scale
                 draw-fn]
  (batch/set-color batch color/white) ; fix scene2d.ui.tooltip flickering
  (batch/set-projection-matrix batch (camera/combined (:camera viewport)))
  (batch/begin batch)
  (sd/with-line-width shape-drawer unit-scale
    (fn []
      (draw-fn (assoc c :cdq.context/unit-scale unit-scale))))
  (batch/end batch))

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

(defn mouse-position [{:keys [cdq.graphics/ui-viewport]}]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position ui-viewport)))

(defn world-mouse-position [{:keys [cdq.graphics/world-viewport]}]
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))

(defn pixels->world-units [{:keys [cdq.graphics/world-unit-scale]} pixels]
  (* (int pixels) world-unit-scale))

(defn- draw-on-world-view* [{:keys [cdq.graphics/world-unit-scale
                                    cdq.graphics/world-viewport] :as c} render-fn]
  (draw-with c
             world-viewport
             world-unit-scale
             render-fn))

(defn draw-on-world-view [render-fns context]
  (draw-on-world-view* context
                       (fn [context]
                         (doseq [f render-fns]
                           (utils/req-resolve-call f context))))
  context)
