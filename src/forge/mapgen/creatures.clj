(ns forge.mapgen.creatures)

(defn with-level [creature-properties level]
  (filter #(= level (:creature/level %)) creature-properties))

(def tile
  (memoize
   (fn [{:keys [property/id] :as prop}]
     (assert id)
     (let [image (property->image prop)
           tile (static-tiled-map-tile (:texture-region image))]
       (put! (m-props tile) "id" id)
       tile))))


