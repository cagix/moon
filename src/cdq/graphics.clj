(ns cdq.graphics
  (:require [cdq.files :as files]
            [cdq.gdx.utils.viewport :as viewport]
            cdq.gdx.graphics
            [cdq.graphics.2d.batch :as batch]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.graphics.camera :as camera]
            [cdq.math.utils :refer [clamp]]
            [cdq.input :as input]
            [cdq.gdx.utils.viewport :as viewport]
            [cdq.graphics.color :as color]
            cdq.graphics.color
            cdq.graphics.pixmap
            cdq.graphics.texture
            [cdq.utils :as utils]))

(defn white-pixel-texture [_context]
  (let [pixmap (doto (cdq.graphics.pixmap/create 1 1 cdq.graphics.pixmap/format-RGBA8888)
                 (cdq.graphics.pixmap/set-color cdq.graphics.color/white)
                 (cdq.graphics.pixmap/draw-pixel 0 0))
        texture (cdq.graphics.texture/create pixmap)]
    (cdq.utils/dispose pixmap)
    texture))

(defrecord Cursors []
  cdq.utils/Disposable
  (dispose [this]
    (run! cdq.utils/dispose (vals this))))

(defn cursors [config {:keys [cdq/files
                              cdq/graphics]}]
  (map->Cursors
   (cdq.utils/mapvals
    (fn [[file [hotspot-x hotspot-y]]]
      (let [pixmap (cdq.graphics.pixmap/create (files/internal files (str "cursors/" file ".png")))
            cursor (cdq.gdx.graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
        (cdq.utils/dispose pixmap)
        cursor))
    config)))

(defn set-cursor [{:keys [cdq/graphics
                          cdq.graphics/cursors]} cursor-key]
  (cdq.gdx.graphics/set-cursor graphics
                                   (utils/safe-get cursors cursor-key)))

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
  [input viewport]
  (let [mouse-x (clamp (input/x input)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (input/y input)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (viewport/unproject viewport mouse-x mouse-y)))

(defn mouse-position [{:keys [cdq.graphics/ui-viewport
                              cdq/input]}]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position input ui-viewport)))

(defn world-mouse-position [{:keys [cdq.graphics/world-viewport
                                    cdq/input]}]
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position input world-viewport))

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
