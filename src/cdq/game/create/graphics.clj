(ns cdq.game.create.graphics
  (:require [cdq.impl.graphics]))

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   params]
  (assoc ctx :ctx/graphics (cdq.impl.graphics/create! gdx params)))
