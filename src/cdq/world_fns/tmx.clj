(ns cdq.world-fns.tmx
  (:require [com.badlogic.gdx.maps.tiled :as tiled]))

(defn create
  [{:keys [tmx-file
           start-position]}]
  {:tiled-map (tiled/tmx-tiled-map tmx-file)
   :start-position start-position})
