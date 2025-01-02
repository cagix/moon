(ns gdl.context
  (:require [clojure.gdx :as gdx :refer [play clamp degree->radians white set-projection-matrix begin end set-color draw unproject input-x input-y]]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.bitmap-font :as font]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.string :as str]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.utils :refer [safe-get pretty-pst with-err-str]]
            [gdl.db :as db]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.sprite :as sprite]
            [gdl.malli :as m]
            [gdl.schema :as schema]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [clojure.gdx.scene2d.group :as group])
  (:import (forge OrthogonalTiledMapRenderer ColorSetter)))

(defn get-sound [{::keys [assets]} sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))

(defn play-sound [c sound-name]
  (play (get-sound c sound-name)))

(defn- texture-region [{::keys [assets]} path]
  (gdx/texture-region (assets path)))

(defn sprite [{::keys [world-unit-scale] :as c} path]
  (sprite/create world-unit-scale
                 (texture-region c path)))

(defn sub-sprite [{::keys [world-unit-scale]} sprite xywh]
  (sprite/sub world-unit-scale
              sprite
              xywh))

(defn sprite-sheet [{::keys [world-unit-scale] :as c} path tilew tileh]
  (sprite/sheet world-unit-scale
                (texture-region c path)
                tilew
                tileh))

(defn from-sprite-sheet [{::keys [world-unit-scale]} sprite-sheet xy]
  (sprite/from-sheet world-unit-scale
                     sprite-sheet
                     xy))

(defn set-cursor [{::keys [cursors] :as c}
                  cursor-key]
  (gdx/set-cursor c (safe-get cursors cursor-key)))

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
  [{::keys [world-viewport tiled-map-renderer]} tiled-map color-setter]
  (draw-tiled-map* (tiled-map-renderer tiled-map)
                   tiled-map
                   color-setter
                   (:camera world-viewport)))

(defn- munge-color [c]
  (cond (= com.badlogic.gdx.graphics.Color (class c)) c
        (keyword? c) (interop/k->color c)
        (vector? c) (apply gdx/color c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn- sd-color [shape-drawer color]
  (sd/set-color shape-drawer (munge-color color)))

(defn ellipse [{::keys [shape-drawer]} [x y] radius-x radius-y color]
  (sd-color shape-drawer color)
  (sd/ellipse shape-drawer x y radius-x radius-y))

(defn filled-ellipse [{::keys [shape-drawer]} [x y] radius-x radius-y color]
  (sd-color shape-drawer color)
  (sd/filled-ellipse shape-drawer x y radius-x radius-y))

(defn circle [{::keys [shape-drawer]} [x y] radius color]
  (sd-color shape-drawer color)
  (sd/circle shape-drawer x y radius))

(defn filled-circle [{::keys [shape-drawer]} [x y] radius color]
  (sd-color shape-drawer color)
  (sd/filled-circle shape-drawer x y radius))

(defn arc [{::keys [shape-drawer]} [center-x center-y] radius start-angle degree color]
  (sd-color shape-drawer color)
  (sd/arc shape-drawer center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn sector [{::keys [shape-drawer]} [center-x center-y] radius start-angle degree color]
  (sd-color shape-drawer color)
  (sd/sector shape-drawer center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn rectangle [{::keys [shape-drawer]} x y w h color]
  (sd-color shape-drawer color)
  (sd/rectangle shape-drawer x y w h))

(defn filled-rectangle [{::keys [shape-drawer]} x y w h color]
  (sd-color shape-drawer color)
  (sd/filled-rectangle shape-drawer x y w h))

(defn line [{::keys [shape-drawer]} [sx sy] [ex ey] color]
  (sd-color shape-drawer color)
  (sd/line shape-drawer sx sy ex ey))

(defn grid [{::keys [shape-drawer]} leftx bottomy gridw gridh cellw cellh color]
  (sd-color shape-drawer color)
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

(defn with-line-width [{::keys [shape-drawer]} width draw-fn]
  (let [old-line-width (sd/default-line-width shape-drawer)]
    (sd/set-default-line-width shape-drawer (* width old-line-width))
    (draw-fn)
    (sd/set-default-line-width shape-drawer old-line-width)))

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
  [{::keys [default-font batch unit-scale]}
   {:keys [font x y text h-align up? scale]}]
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
  (if color (set-color batch color))
  (draw batch
        texture-region
        :x x
        :y y
        :origin-x (/ (float w) 2) ; rotation origin
        :origin-y (/ (float h) 2)
        :width w
        :height h
        :scale-x 1
        :scale-y 1
        :rotation rotation)
  (if color (set-color batch white)))

(defn draw-image
  [{::keys [batch unit-scale]} {:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image unit-scale)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{::keys [batch unit-scale]} {:keys [texture-region color] :as image} rotation [x y]]
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
  (set-color batch white) ; fix scene2d.ui.tooltip flickering
  (set-projection-matrix batch (camera/combined (:camera viewport)))
  (begin batch)
  (draw-fn)
  (end batch))

(defn draw-with [{::keys [batch] :as c} viewport unit-scale draw-fn]
  (draw-on-viewport batch
                    viewport
                    #(with-line-width c unit-scale
                       (fn []
                         (draw-fn (assoc c ::unit-scale unit-scale))))))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [c viewport]
  (let [mouse-x (clamp (input-x c)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (input-y c)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (unproject viewport mouse-x mouse-y)))

(defn mouse-position [{::keys [viewport] :as c}]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position c viewport)))

(defn world-mouse-position [{::keys [world-viewport] :as c}]
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position c world-viewport))

(defn pixels->world-units [{::keys [world-unit-scale]} pixels]
  (* (int pixels) world-unit-scale))

(defn draw-on-world-view [{::keys [world-viewport world-unit-scale] :as c} render-fn]
  (draw-with c world-viewport world-unit-scale render-fn))

(def stage ::stage)

(defn build [{::keys [db] :as c} id]
  (db/build db id c))

(defn build-all [{::keys [db] :as c} property-type]
  (db/build-all db property-type c))

(defn add-actor [{::keys [stage]} actor]
  (stage/add-actor stage actor))

(defn reset-stage [{::keys [stage]} new-actors]
  (stage/clear stage)
  (run! #(stage/add-actor stage %) new-actors))

(defn mouse-on-actor? [{::keys [stage] :as c}]
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
