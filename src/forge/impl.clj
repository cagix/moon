(ns forge.impl
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.viewport :as vp]
            [clojure.pprint :as pprint]
            [forge.app.asset-manager :refer [asset-manager]]
            [forge.app.db :as db]
            [forge.app.world-viewport :refer [world-unit-scale]]
            [forge.core :refer :all]
            [forge.val-max :as val-max]
            [malli.core :as m]
            [malli.generator :as mg])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defmethod db/malli-form :s/val-max [_]
  (m/form val-max/schema))

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
