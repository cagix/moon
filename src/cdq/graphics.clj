(ns cdq.graphics
  (:require [gdl.graphics :as graphics]))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} scale world-unit-scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (graphics/dimensions texture-region)
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

(defn sub-sprite [sprite [x y w h] world-unit-scale]
  (sprite* (graphics/sub-region (:texture-region sprite) x y w h)
           world-unit-scale))

(defn sprite-sheet [texture tilew tileh world-unit-scale]
  {:image (sprite* (graphics/texture-region texture)
                   world-unit-scale)
   :tilew tilew
   :tileh tileh})

(defn from-sheet [{:keys [image tilew tileh]} [x y] world-unit-scale]
  (sub-sprite image
              [(* x tilew) (* y tileh) tilew tileh]
              world-unit-scale))

(defn sprite [texture world-unit-scale]
  (sprite* (graphics/texture-region texture)
           world-unit-scale))
