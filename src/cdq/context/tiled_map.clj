(ns cdq.context.tiled-map
  (:require [clojure.gdx :as gdx]))

(defn create [_ {:keys [cdq.context/level]}]
  (:tiled-map level))

(defn dispose [[_ tiled-map]]
  (gdx/dispose tiled-map))
