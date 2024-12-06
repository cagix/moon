(ns forge.impl
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.viewport :as vp]
            [clojure.pprint :as pprint]
            [forge.app.asset-manager :refer [asset-manager]]
            [forge.app.db :as db]
            [forge.app.world-viewport :refer [world-unit-scale]]
            [forge.core :refer :all]
            [malli.core :as m]
            [malli.generator :as mg])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.math Circle Intersector Rectangle)))

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
