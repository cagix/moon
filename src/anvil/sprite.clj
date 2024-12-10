(ns anvil.sprite
  (:require [anvil.assets :as assets]
            [anvil.world :as world]
            [clojure.gdx :as gdx]))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- texture-dimensions [texture-region]
  [(gdx/region-width  texture-region)
   (gdx/region-height texture-region)])

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (texture-dimensions texture-region) scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world/unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- create* [texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1) ; = scale 1
      map->Sprite))

(defn create [path]
  (create* (gdx/texture-region (assets/manager path))))

(defn sub [image bounds]
  (create* (apply gdx/->texture-region (:texture-region image) bounds)))

(defn sheet [path tilew tileh]
  {:image (create path)
   :tilew tilew
   :tileh tileh})

(defn from-sheet [{:keys [image tilew tileh]} [x y]]
  (sub image
       [(* x tilew)
        (* y tileh)
        tilew
        tileh]))
