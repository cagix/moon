(ns clojure.context
  (:require cdq.graphics
            [clojure.scene2d.stage :as stage]))

(defn add-actor [{:keys [clojure.context/stage]} actor]
  (stage/add-actor stage actor))

(defn reset-stage [{:keys [clojure.context/stage]} new-actors]
  (stage/clear stage)
  (run! #(stage/add-actor stage %) new-actors))

(defn mouse-on-actor? [{:keys [clojure.context/stage] :as c}]
  (let [[x y] (cdq.graphics/mouse-position c)]
    (stage/hit stage x y true)))
