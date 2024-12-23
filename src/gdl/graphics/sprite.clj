(ns gdl.graphics.sprite
  (:require [clojure.gdx.graphics.g2d.texture-region :as texture-region]))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} world-unit-scale scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (texture-region/dimensions texture-region) scale)
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

(defn create [{:keys [assets world-unit-scale]} path]
  (create* world-unit-scale
           (texture-region/create (assets path))))

(defn sub [world-unit-scale image bounds]
  (create* world-unit-scale
           (apply texture-region/->create (:texture-region image) bounds)))

(defn sheet [ctx path tilew tileh]
  {:image (create ctx path)
   :tilew tilew
   :tileh tileh})

(defn from-sheet [world-unit-scale {:keys [image tilew tileh]} [x y]]
  (sub world-unit-scale
       image
       [(* x tilew)
        (* y tileh)
        tilew
        tileh]))
