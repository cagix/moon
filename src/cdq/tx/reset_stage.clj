(ns cdq.tx.reset-stage
  (:require [gdl.scene2d :as scene2d]
            [gdl.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage
           ctx/ui-actors]
    :as ctx}]
  (stage/clear! stage)
  (let [actors (map #(let [[f & params] %]
                       (apply f ctx params))
                    ui-actors)]
    (doseq [actor actors]
      (stage/add! stage (scene2d/build actor))))
  nil)
