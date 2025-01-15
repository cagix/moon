(ns clojure.graphics.sprite
  (:require [clojure.graphics.2d.texture-region :as texture-region]))

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

(defn create [world-unit-scale texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions world-unit-scale 1) ; = scale 1
      map->Sprite))

(defn sub [sprite bounds {:keys [clojure.graphics/world-unit-scale]}]
  (create world-unit-scale
          (apply texture-region/->create (:texture-region sprite) bounds)))

(defn sheet [{:keys [clojure.graphics/world-unit-scale
                     clojure/assets]}
             path
             tilew
             tileh]
  {:image (create world-unit-scale
                  (texture-region/create (assets path)))
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
