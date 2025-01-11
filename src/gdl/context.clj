(ns gdl.context
  (:require [clojure.gdx.graphics.camera :as camera]
            [gdl.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.bitmap-font :as font]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.interop :as interop]
            [gdl.scene2d.stage :as stage]
            [clojure.gdx.math.vector2 :as v]
            [clojure.gdx.math.utils :refer [clamp degree->radians]]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.graphics :as graphics]
            [clojure.graphics.2d.batch :as batch]
            [gdl.input :as input]
            [clojure.string :as str]
            [gdl.utils :refer [defcomponent safe-get with-err-str mapvals]]
            [gdl.audio :as audio]
            [cdq.db :as db]
            [cdq.error :refer [pretty-pst]]
            [cdq.graphics.animation :as animation]
            [cdq.graphics.sprite :as sprite]
            [gdl.graphics.camera :as cam]
            [cdq.malli :as m]
            [cdq.schema :as schema]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.scene2d.group :as group])
  (:import (gdl OrthogonalTiledMapRenderer ColorSetter)))

(defn get-sound [{:keys [gdl/assets]} sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))

(defn play-sound [c sound-name]
  (audio/play (get-sound c sound-name)))

(defn- texture-region [{:keys [gdl/assets]} path]
  (texture-region/create (assets path)))

(defn sprite [{:keys [context/g] :as c} path]
  (sprite/create (:world-unit-scale g)
                 (texture-region c path)))

(defn sub-sprite [{:keys [context/g]} sprite xywh]
  (sprite/sub (:world-unit-scale g)
              sprite
              xywh))

(defn sprite-sheet [{:keys [context/g] :as c} path tilew tileh]
  (sprite/sheet (:world-unit-scale g)
                (texture-region c path)
                tilew
                tileh))

(defn from-sprite-sheet [{:keys [context/g]} sprite-sheet xy]
  (sprite/from-sheet (:world-unit-scale g)
                     sprite-sheet
                     xy))

(defn set-cursor [{:keys [clojure/graphics context/g]}
                  cursor-key]
  (graphics/set-cursor graphics (safe-get (:cursors g) cursor-key)))

(defn- draw-tiled-map* [^OrthogonalTiledMapRenderer this tiled-map color-setter camera]
  (.setColorSetter this (reify ColorSetter
                          (apply [_ color x y]
                            (color-setter color x y))))
  (.setView this camera)
  (->> tiled-map
       tiled/layers
       (filter tiled/visible?)
       (map (partial tiled/layer-index tiled-map))
       int-array
       (.render this)))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [{:keys [context/g]} tiled-map color-setter]
  (draw-tiled-map* ((:tiled-map-renderer g) tiled-map)
                   tiled-map
                   color-setter
                   (:camera (:world-viewport g))))

(defn- munge-color [c]
  (cond (= com.badlogic.gdx.graphics.Color (class c)) c
        (keyword? c) (interop/k->color c)
        (vector? c) (apply color/create c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn- sd-color [shape-drawer color]
  (sd/set-color shape-drawer (munge-color color)))

(defn ellipse [{:keys [context/g]} [x y] radius-x radius-y color]
  (sd-color (:sd g) color)
  (sd/ellipse (:sd g) x y radius-x radius-y))

(defn filled-ellipse [{:keys [context/g]} [x y] radius-x radius-y color]
  (sd-color (:sd g) color)
  (sd/filled-ellipse (:sd g) x y radius-x radius-y))

(defn circle [{:keys [context/g]} [x y] radius color]
  (sd-color (:sd g) color)
  (sd/circle (:sd g) x y radius))

(defn filled-circle [{:keys [context/g]} [x y] radius color]
  (sd-color (:sd g) color)
  (sd/filled-circle (:sd g) x y radius))

(defn arc [{:keys [context/g]} [center-x center-y] radius start-angle degree color]
  (sd-color (:sd g) color)
  (sd/arc (:sd g) center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn sector [{:keys [context/g]} [center-x center-y] radius start-angle degree color]
  (sd-color (:sd g) color)
  (sd/sector (:sd g) center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn rectangle [{:keys [context/g]} x y w h color]
  (sd-color (:sd g) color)
  (sd/rectangle (:sd g) x y w h))

(defn filled-rectangle [{:keys [context/g]} x y w h color]
  (sd-color (:sd g) color)
  (sd/filled-rectangle (:sd g) x y w h))

(defn line [{:keys [context/g]} [sx sy] [ex ey] color]
  (sd-color (:sd g) color)
  (sd/line (:sd g) sx sy ex ey))

(defn grid [{:keys [context/g]} leftx bottomy gridw gridh cellw cellh color]
  (sd-color (:sd g) color)
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line [linex topy] [linex bottomy]))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line [leftx liney] [rightx liney]))))

(defn with-line-width [{:keys [context/g]} width draw-fn]
  (let [old-line-width (sd/default-line-width (:sd g))]
    (sd/set-default-line-width (:sd g) (* width old-line-width))
    (draw-fn)
    (sd/set-default-line-width (:sd g) old-line-width)))

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
  [{:keys [gdl.context/unit-scale
           context/g]}
   {:keys [font x y text h-align up? scale]}]
  {:pre [unit-scale]}
  (let [font (or font (:default-font g))
        data (font/data font)
        old-scale (float (font/scale-x data))]
    (font/set-scale data (* old-scale
                            (float unit-scale)
                            (float (or scale 1))))
    (font/draw :font font
               :batch (:batch g)
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
  [{:keys [gdl.context/unit-scale
           context/g]}
   {:keys [texture-region color] :as image} position]
  (draw-texture-region (:batch g)
                       texture-region
                       position
                       (unit-dimensions image unit-scale)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [gdl.context/unit-scale
           context/g]}
   {:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image unit-scale)]
    (draw-texture-region (:batch g)
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

(defn draw-with [{:keys [context/g] :as c} viewport unit-scale draw-fn]
  (draw-on-viewport (:batch g)
                    viewport
                    #(with-line-width c unit-scale
                       (fn []
                         (draw-fn (assoc c :gdl.context/unit-scale unit-scale))))))

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

(defn mouse-position [{:keys [context/g
                              clojure/input]}]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position input (:ui-viewport g))))

(defn world-mouse-position [{:keys [context/g clojure/input]}]
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position input (:world-viewport g)))

(defn pixels->world-units [{:keys [context/g]} pixels]
  (* (int pixels) (:world-unit-scale g)))

(defn draw-on-world-view [{:keys [context/g] :as c} render-fn]
  (draw-with c
             (:world-viewport g)
             (:world-unit-scale g)
             render-fn))

(def stage :gdl.context/stage)

(defn build [{:keys [gdl.context/db] :as c} id]
  (db/build db id c))

(defn build-all [{:keys [gdl.context/db] :as c} property-type]
  (db/build-all db property-type c))

(defn add-actor [{:keys [gdl.context/stage]} actor]
  (stage/add-actor stage actor))

(defn reset-stage [{:keys [gdl.context/stage]} new-actors]
  (stage/clear stage)
  (run! #(stage/add-actor stage %) new-actors))

(defn mouse-on-actor? [{:keys [gdl.context/stage] :as c}]
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

; TODO do we care in here about malli-form ?! - where used? - hide inside 'schemas' ? or schemas/validation

(defmethod schema/malli-form :s/val-max [_ _schemas] m/val-max-schema)
(defmethod schema/malli-form :s/number  [_ _schemas] m/number-schema)
(defmethod schema/malli-form :s/nat-int [_ _schemas] m/nat-int-schema)
(defmethod schema/malli-form :s/int     [_ _schemas] m/int-schema)
(defmethod schema/malli-form :s/pos     [_ _schemas] m/pos-schema)
(defmethod schema/malli-form :s/pos-int [_ _schemas] m/pos-int-schema)

(defcomponent :s/sound
  (schema/malli-form [_ _schemas]
    m/string-schema)

  (db/edn->value [_ sound-name _db c]
    (get-sound c sound-name)))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (from-sprite-sheet c
                         (sprite-sheet c file tilew tileh)
                         [(int (/ sprite-x tilew))
                          (int (/ sprite-y tileh))]))
    (sprite c file)))

(defcomponent :s/image
  (schema/malli-form  [_ _schemas]
    m/image-schema)

  (db/edn->value [_ edn _db c]
    (edn->sprite c edn)))

(defcomponent :s/animation
  (schema/malli-form [_ _schemas]
    m/animation-schema)

  (db/edn->value [_ {:keys [frames frame-duration looping?]} _db c]
    (animation/create (map #(edn->sprite c %) frames)
                      :frame-duration frame-duration
                      :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defcomponent :s/one-to-one
  (schema/malli-form [[_ property-type] _schemas]
    (m/qualified-keyword-schema (type->id-namespace property-type)))
  (db/edn->value [_ property-id db c]
    (build c property-id)))

(defcomponent :s/one-to-many
  (schema/malli-form [[_ property-type] _schemas]
    (m/set-schema (m/qualified-keyword-schema (type->id-namespace property-type))))
  (db/edn->value [_ property-ids db c]
    (set (map #(build c %) property-ids))))

(defn- map-form [ks schemas]
  (m/map-schema ks (fn [k]
                     (schema/malli-form (schema/schema-of schemas k)
                                        schemas))))
; TODO schema/validate comes to this...
; but db-data is not yet existing?

(defmethod schema/malli-form :s/map [[_ ks] schemas]
  (map-form ks schemas))

(defmethod schema/malli-form :s/map-optional [[_ ks] schemas]
  (map-form (map (fn [k] [k {:optional true}]) ks)
            schemas))

(defn- namespaced-ks [schemas ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod schema/malli-form :s/components-ns [[_ ns-name-k] schemas]
  (schema/malli-form [:s/map-optional (namespaced-ks schemas ns-name-k)]
                     schemas))

(defn WASD-movement-vector [input]
  (let [r (when (input/key-pressed? input :d) [1  0])
        l (when (input/key-pressed? input :a) [-1 0])
        u (when (input/key-pressed? input :w) [0  1])
        d (when (input/key-pressed? input :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(def ^:private zoom-speed 0.025)

(defn check-camera-controls [{:keys [context/g
                                     clojure/input]
                              :as context}]
  (let [camera (:camera (:world-viewport g))]
    (when (input/key-pressed? input :minus)  (cam/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? input :equals) (cam/inc-zoom camera (- zoom-speed))))
  context)

(defn update-stage [context]
   (ui/act (stage context) context)
   context)
