(ns forge.impl
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.math.utils :refer [equal?]]
            [clojure.gdx.utils.viewport :as vp]
            [clojure.pprint :as pprint]
            [data.grid2d :as g2d]
            [forge.app.asset-manager :refer [asset-manager]]
            [forge.app.db :as db]
            [forge.app.world-viewport :refer [world-unit-scale]]
            [forge.core :refer :all]
            [malli.core :as m]
            [malli.generator :as mg])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.utils.viewport Viewport)
           (com.badlogic.gdx.math Vector2 Circle Intersector Rectangle)))

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

(defn-impl ->image [path]
  (sprite* world-unit-scale
           (g/texture-region (asset-manager path))))

(defn-impl sub-image [image bounds]
  (sprite* world-unit-scale
           (apply g/->texture-region (:texture-region image) bounds)))

(defn-impl sprite-sheet [path tilew tileh]
  {:image (->image path)
   :tilew tilew
   :tileh tileh})

(defn-impl ->sprite [{:keys [image tilew tileh]} [x y]]
  (sub-image image
             [(* x tilew) (* y tileh) tilew tileh]))
