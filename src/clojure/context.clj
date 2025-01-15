(ns clojure.context
  (:require [clojure.graphics.camera :as camera]
            [clojure.graphics.color :as color]
            [clojure.graphics.shape-drawer :as sd]
            [clojure.graphics.2d.bitmap-font :as font]
            [clojure.graphics.2d.texture-region :as texture-region]
            [clojure.interop :as interop]
            [clojure.graphics.tiled-map-renderer :as tiled-map-renderer]
            [clojure.scene2d.stage :as stage]
            [clojure.math.utils :refer [clamp]]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.graphics :as graphics]
            [clojure.graphics.2d.batch :as batch]
            [clojure.input :as input]
            [clojure.string :as str]
            [clojure.utils :refer [safe-get with-err-str mapvals]]
            [clojure.audio :as audio]
            [clojure.db :as db]
            [clojure.error :refer [pretty-pst]]
            [clojure.graphics.sprite :as sprite]
            [clojure.ui :as ui]
            [clojure.scene2d.group :as group]))

(defn get-sound [{:keys [clojure/assets]} sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))

(defn play-sound [c sound-name]
  (audio/play (get-sound c sound-name)))

(defn sprite [{:keys [clojure.graphics/world-unit-scale
                      clojure/assets]}
              path]
  (sprite/create world-unit-scale
                 (texture-region/create (assets path))))

(defn set-cursor [{:keys [clojure/graphics
                          clojure.graphics/cursors]} cursor-key]
  (graphics/set-cursor graphics (safe-get cursors cursor-key)))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [{:keys [clojure.graphics/tiled-map-renderer
           clojure.graphics/world-viewport]}
   tiled-map
   color-setter]
  (tiled-map-renderer/draw (tiled-map-renderer tiled-map)
                           tiled-map
                           color-setter
                           (:camera world-viewport)))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (font/line-height font))))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [clojure.context/unit-scale
           clojure.graphics/batch
           clojure.graphics/default-font]}
   {:keys [font x y text h-align up? scale]}]
  {:pre [unit-scale]}
  (let [font (or font default-font)
        data (font/data font)
        old-scale (float (font/scale-x data))]
    (font/set-scale data (* old-scale
                            (float unit-scale)
                            (float (or scale 1))))
    (font/draw :font font
               :batch batch
               :text text
               :x x
               :y (+ y (if up? (text-height font text) 0))
               :align (interop/k->align (or h-align :center)))
    (font/set-scale data old-scale)))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- draw-texture-region [batch texture-region [x y] [w h] rotation color]
  (if color (batch/set-color batch color))
  (batch/draw batch
              texture-region
              {:x x
               :y y
               :origin-x (/ (float w) 2) ; rotation origin
               :origin-y (/ (float h) 2)
               :width w
               :height h
               :scale-x 1
               :scale-y 1
               :rotation rotation})
  (if color (batch/set-color batch color/white)))

(defn draw-image
  [{:keys [clojure.context/unit-scale
           clojure.graphics/batch]}
   {:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image unit-scale)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [clojure.context/unit-scale
           clojure.graphics/batch]}
   {:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image unit-scale)]
    (draw-texture-region batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn draw-centered [c image position]
  (draw-rotated-centered c image 0 position))

(defn- draw-on-viewport [batch viewport draw-fn]
  (batch/set-color batch color/white) ; fix scene2d.ui.tooltip flickering
  (batch/set-projection-matrix batch (camera/combined (:camera viewport)))
  (batch/begin batch)
  (draw-fn)
  (batch/end batch))

(defn draw-with [{:keys [clojure.graphics/batch
                         clojure.graphics/shape-drawer] :as c}
                 viewport
                 unit-scale
                 draw-fn]
  (draw-on-viewport batch
                    viewport
                    #(sd/with-line-width shape-drawer unit-scale
                       (fn []
                         (draw-fn (assoc c :clojure.context/unit-scale unit-scale))))))

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

(defn draw-on-world-view [{:keys [clojure.graphics/world-unit-scale
                                  clojure.graphics/world-viewport] :as c} render-fn]
  (draw-with c
             world-viewport
             world-unit-scale
             render-fn))

(def stage :clojure.context/stage)

(defn build [{:keys [clojure/db] :as c} id]
  (db/build db id c))

(defn build-all [{:keys [clojure/db] :as c} property-type]
  (db/build-all db property-type c))

(defn add-actor [{:keys [clojure.context/stage]} actor]
  (stage/add-actor stage actor))

(defn reset-stage [{:keys [clojure.context/stage]} new-actors]
  (stage/clear stage)
  (run! #(stage/add-actor stage %) new-actors))

(defn mouse-on-actor? [{:keys [clojure.context/stage] :as c}]
  (let [[x y] (mouse-position c)]
    (stage/hit stage x y true)))

(defn error-window [c throwable]
  (pretty-pst throwable)
  (add-actor c
   (ui/window {:title "Error"
               :rows [[(ui/label (binding [*print-level* 3]
                                   (with-err-str
                                     (clojure.repl/pst throwable))))]]
               :modal? true
               :close-button? true
               :close-on-escape? true
               :center? true
               :pack? true})))
