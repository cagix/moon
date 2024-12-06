(ns forge.impl
  (:require [clj-commons.pretty.repl :as pretty-repl]
            [clojure.gdx.interop :refer [static-field]]
            [clojure.gdx.math.utils :refer [equal?]]
            [clojure.gdx.utils.viewport :as vp]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [data.grid2d :as g2d]
            [forge.app.asset-manager :refer [asset-manager]]
            [forge.app.db :as db]
            [forge.app.default-font :refer [default-font]]
            [forge.core :refer :all]
            [malli.core :as m]
            [malli.generator :as mg])
  (:import (com.badlogic.gdx.graphics Color Colors Texture)
           (com.badlogic.gdx.graphics.g2d BitmapFont TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.utils Align Scaling)
           (com.badlogic.gdx.utils.viewport Viewport)
           (com.badlogic.gdx.math Vector2 Circle Intersector Rectangle)))

(defn-impl pretty-pst [t]
  (binding [*print-level* 3]
    (pretty-repl/pretty-pst t 24)))

(extend-type Actor
  HasUserObject
  (user-object [actor]
    (.getUserObject actor)))

(extend-type com.badlogic.gdx.scenes.scene2d.Group
  Group
  (children [group]
    (seq (.getChildren group)))

  (clear-children [group]
    (.clearChildren group))

  (add-actor! [group actor]
    (.addActor group actor))

  (find-actor [group name]
    (.findActor group name)))

(def-impl black Color/BLACK)
(def-impl white Color/WHITE)

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- gdx-align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn- gdx-scaling [k]
  (case k
    :fill Scaling/fill))

(defn-impl draw-text
  [{:keys [font x y text h-align up? scale]}]
  (let [^BitmapFont font (or font default-font)
        data (.getData font)
        old-scale (float (.scaleX data))]
    (.setScale data (* old-scale
                       (float *unit-scale*)
                       (float (or scale 1))))
    (.draw font
           batch
           (str text)
           (float x)
           (+ (float y) (float (if up? (text-height font text) 0)))
           (float 0) ; target-width
           (gdx-align (or h-align :center))
           false) ; wrap false, no need target-width
    (.setScale data old-scale)))

(defn-impl ->color
  ([r g b]
   (->color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a)))
  (^Color [c]
          (cond (= Color (class c)) c
                (keyword? c) (static-field "graphics.Color" c)
                (vector? c) (apply ->color c)
                :else (throw (ex-info "Cannot understand color" c)))))

(extend-type Actor
  HasVisible
  (set-visible [actor bool]
    (.setVisible actor bool))
  (visible? [actor]
    (.isVisible actor)))

(defn-impl add-actor [actor]
  (.addActor (screen-stage) actor))

(defn-impl reset-stage [new-actors]
  (.clear (screen-stage))
  (run! add-actor new-actors))

(def-impl grid2d                    g2d/create-grid)
(def-impl g2d-width                 g2d/width)
(def-impl g2d-height                g2d/height)
(def-impl g2d-cells                 g2d/cells)
(def-impl g2d-posis                 g2d/posis)
(def-impl get-4-neighbour-positions g2d/get-4-neighbour-positions)
(def-impl mapgrid->vectorgrid       g2d/mapgrid->vectorgrid)

(defn- m-v2
  (^Vector2 [[x y]] (Vector2. x y))
  (^Vector2 [x y]   (Vector2. x y)))

(defn- ->p [^Vector2 v]
  [(.x v) (.y v)])

(defn-impl v-scale [v n]
  (->p (.scl (m-v2 v) (float n))))

(defn-impl v-normalise [v]
  (->p (.nor (m-v2 v))))

(defn-impl v-add [v1 v2]
  (->p (.add (m-v2 v1) (m-v2 v2))))

(defn-impl v-length [v]
  (.len (m-v2 v)))

(defn-impl v-distance [v1 v2]
  (.dst (m-v2 v1) (m-v2 v2)))

(defn-impl v-normalised? [v]
  (equal? 1 (v-length v)))

(defn-impl v-direction [[sx sy] [tx ty]]
  (v-normalise [(- (float tx) (float sx))
                (- (float ty) (float sy))]))

(defn-impl v-angle-from-vector [v]
  (.angleDeg (m-v2 v) (Vector2. 0 1)))

(comment

 (pprint
  (for [v [[0 1]
           [1 1]
           [1 0]
           [1 -1]
           [0 -1]
           [-1 -1]
           [-1 0]
           [-1 1]]]
    [v
     (.angleDeg (m-v2 v) (Vector2. 0 1))
     (get-angle-from-vector (m-v2 v))]))

 )

(defn- m->shape [m]
  (cond
   (rectangle? m) (let [{:keys [left-bottom width height]} m
                        [x y] left-bottom]
                    (Rectangle. x y width height))

   (circle? m) (let [{:keys [position radius]} m
                     [x y] position]
                 (Circle. x y radius))

   :else (throw (Error. (str m)))))

(defmulti ^:private overlaps?* (fn [a b] [(class a) (class b)]))

(defmethod overlaps?* [Circle Circle]
  [^Circle a ^Circle b]
  (Intersector/overlaps a b))

(defmethod overlaps?* [Rectangle Rectangle]
  [^Rectangle a ^Rectangle b]
  (Intersector/overlaps a b))

(defmethod overlaps?* [Rectangle Circle]
  [^Rectangle rect ^Circle circle]
  (Intersector/overlaps circle rect))

(defmethod overlaps?* [Circle Rectangle]
  [^Circle circle ^Rectangle rect]
  (Intersector/overlaps circle rect))

(defn-impl overlaps? [a b]
  (overlaps?* (m->shape a) (m->shape b)))

(defn-impl rect-contains? [rectangle [x y]]
  (Rectangle/.contains (m->shape rectangle) x y))

(def-impl val-max-schema
  (m/schema [:and
             [:vector {:min 2 :max 2} [:int {:min 0}]]
             [:fn {:error/fn (fn [{[^int v ^int mx] :value} _]
                               (when (< mx v)
                                 (format "Expected max (%d) to be smaller than val (%d)" v mx)))}
              (fn [[^int a ^int b]] (<= a b))]]))

(defmethod db/malli-form :s/val-max [_]
  (m/form val-max-schema))

(defn-impl val-max-ratio
  [[^int v ^int mx]]
  {:pre [(m/validate val-max-schema [v mx])]}
  (if (and (zero? v) (zero? mx))
    0
    (/ v mx)))

(defn-impl add-color [name-str color]
  (Colors/put name-str (->color color)))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- texture-dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} world-unit-scale scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (texture-dimensions texture-region) scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defn- sprite* [world-unit-scale texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions world-unit-scale 1) ; = scale 1
      map->Sprite))

(extend-type com.badlogic.gdx.graphics.g2d.Batch
  Batch
  (draw-texture-region [this texture-region [x y] [w h] rotation color]
    (if color (.setColor this color))
    (.draw this
           texture-region
           x
           y
           (/ (float w) 2) ; rotation origin
           (/ (float h) 2)
           w ; width height
           h
           1 ; scaling factor
           1
           rotation)
    (if color (.setColor this white)))

  (draw-on-viewport [this viewport draw-fn]
    (.setColor this white) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix this (.combined (Viewport/.getCamera viewport)))
    (.begin this)
    (draw-fn)
    (.end this)))

(defn-impl world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (vp/unproject-mouse-position world-viewport))

(defn-impl world-camera []
  (Viewport/.getCamera world-viewport))

(defn-impl ->texture-region
  ([path]
   (TextureRegion. ^Texture (asset-manager path)))
  ([^TextureRegion texture-region [x y w h]]
   (TextureRegion. texture-region (int x) (int y) (int w) (int h))))

(defn-impl ->image [path]
  (sprite* world-unit-scale
           (->texture-region path)))

(defn-impl sub-image [image bounds]
  (sprite* world-unit-scale
           (->texture-region (:texture-region image) bounds)))

(defn-impl sprite-sheet [path tilew tileh]
  {:image (->image path)
   :tilew tilew
   :tileh tileh})

(defn-impl ->sprite [{:keys [image tilew tileh]} [x y]]
  (sub-image image
             [(* x tilew) (* y tileh) tilew tileh]))

(defn-impl draw-image [{:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image *unit-scale*)
                       0 ; rotation
                       color))

(defn-impl draw-rotated-centered
  [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image *unit-scale*)]
    (draw-texture-region batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn-impl draw-centered [image position]
  (draw-rotated-centered image 0 position))

(defn- draw-with [viewport unit-scale draw-fn]
  (draw-on-viewport batch
                    viewport
                    #(with-line-width unit-scale
                       (fn []
                         (binding [*unit-scale* unit-scale]
                           (draw-fn))))))

(defn-impl draw-on-world-view [render-fn]
  (draw-with world-viewport
             world-unit-scale
             render-fn))
