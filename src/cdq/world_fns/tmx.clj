(ns cdq.world-fns.tmx
  (:require [clojure.gdx.maps.tiled.tmx :as tmx]))

(defn create
  [{:keys [tmx-file
           start-position]}]
  {:tiled-map (tmx/load-map tmx-file)
   :start-position start-position})
