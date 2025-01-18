(ns cdq.stage
  (:require [cdq.graphics :as graphics]
            [cdq.scene2d.stage :as stage]))

(defn mouse-on-actor? [{:keys [cdq.context/stage] :as c}]
  (let [[x y] (graphics/mouse-position c)]
    (stage/hit stage x y true)))

(defn add-actor [{:keys [cdq.context/stage]} actor]
  (stage/add-actor stage actor))

(defn reset-stage [{:keys [cdq.context/stage]} new-actors]
  (stage/clear stage)
  (run! #(stage/add-actor stage %) new-actors))
