(ns mapgen.creatures
  (:require [clojure.gdx.tiled :as t]
            [forge.db :as db]))

(defn with-level [creature-properties level]
  (filter #(= level (:creature/level %)) creature-properties))

(def tile
  (memoize
   (fn [{:keys [property/id] :as prop}]
     (assert id)
     (let [image (db/property->image prop)
           tile (t/static-tiled-map-tile (:texture-region image))]
       (t/put! (t/m-props tile) "id" id)
       tile))))


