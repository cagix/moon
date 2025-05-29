(ns cdq.create.graphics
  (:require [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/gdx
                   ctx/config]
            :as ctx}]
  (assoc ctx :ctx/graphics (graphics/create gdx config)))
