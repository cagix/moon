(ns cdq.graphics
  (:require [clojure.files :as files]
            [clojure.gdx.utils.viewport :as viewport]
            clojure.gdx.graphics
            [clojure.graphics.2d.batch :as batch]
            [clojure.graphics.shape-drawer :as sd]
            [clojure.graphics.camera :as camera]
            [clojure.math.utils :refer [clamp]]
            [clojure.input :as input]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.graphics.color :as color]
            clojure.graphics.color
            clojure.graphics.pixmap
            clojure.graphics.texture
            [clojure.utils :as utils]))

(defn white-pixel-texture [_context]
  (let [pixmap (doto (clojure.graphics.pixmap/create 1 1 clojure.graphics.pixmap/format-RGBA8888)
                 (clojure.graphics.pixmap/set-color clojure.graphics.color/white)
                 (clojure.graphics.pixmap/draw-pixel 0 0))
        texture (clojure.graphics.texture/create pixmap)]
    (clojure.utils/dispose pixmap)
    texture))

(defn resize-viewports [context width height]
  (viewport/update (:clojure.graphics/ui-viewport    context) width height :center-camera? true)
  (viewport/update (:clojure.graphics/world-viewport context) width height))

(defrecord Cursors []
  clojure.utils/Disposable
  (dispose [this]
    (run! clojure.utils/dispose (vals this))))

(defn cursors [config {:keys [clojure/files
                              clojure/graphics]}]
  (map->Cursors
   (clojure.utils/mapvals
    (fn [[file [hotspot-x hotspot-y]]]
      (let [pixmap (clojure.graphics.pixmap/create (files/internal files (str "cursors/" file ".png")))
            cursor (clojure.gdx.graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
        (clojure.utils/dispose pixmap)
        cursor))
    config)))

(defn set-cursor [{:keys [clojure/graphics
                          clojure.graphics/cursors]} cursor-key]
  (clojure.gdx.graphics/set-cursor graphics
                                   (utils/safe-get cursors cursor-key)))

(defn- draw-with [{:keys [clojure.graphics/batch
                          clojure.graphics/shape-drawer] :as c}
                 viewport
                 unit-scale
                 draw-fn]
  (batch/set-color batch color/white) ; fix scene2d.ui.tooltip flickering
  (batch/set-projection-matrix batch (camera/combined (:camera viewport)))
  (batch/begin batch)
  (sd/with-line-width shape-drawer unit-scale
    (fn []
      (draw-fn (assoc c :clojure.context/unit-scale unit-scale))))
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

(defn mouse-position [{:keys [clojure.graphics/ui-viewport
                              clojure/input]}]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position input ui-viewport)))

(defn world-mouse-position [{:keys [clojure.graphics/world-viewport
                                    clojure/input]}]
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position input world-viewport))

(defn pixels->world-units [{:keys [clojure.graphics/world-unit-scale]} pixels]
  (* (int pixels) world-unit-scale))

(defn- draw-on-world-view* [{:keys [clojure.graphics/world-unit-scale
                                    clojure.graphics/world-viewport] :as c} render-fn]
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
