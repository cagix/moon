(ns cdq.graphics.sprite
  (:require [cdq.assets :as assets])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [^TextureRegion texture-region] :as image} world-unit-scale scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions [(.getRegionWidth  texture-region)
                                              (.getRegionHeight texture-region)]
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- create* [world-unit-scale texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions world-unit-scale 1) ; = scale 1
      map->Sprite))

(defn sub [sprite [x y w h] {:keys [cdq.graphics/world-unit-scale]}]
  (create* world-unit-scale
           (TextureRegion. ^TextureRegion (:texture-region sprite) (int x) (int y) (int w) (int h))))

(defn sheet [{:keys [cdq.graphics/world-unit-scale]}
             path
             tilew
             tileh]
  {:image (create* world-unit-scale
                   (TextureRegion. ^Texture (assets/get path)))
   :tilew tilew
   :tileh tileh})

(defn from-sheet [{:keys [image tilew tileh]}
                  [x y]
                  context]
  (sub image
       [(* x tilew)
        (* y tileh)
        tilew
        tileh]
       context))

(defn create [{:keys [cdq.graphics/world-unit-scale]}
              path]
  (create* world-unit-scale
           (TextureRegion. ^Texture (assets/get path))))
