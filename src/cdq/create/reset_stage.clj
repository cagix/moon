(ns cdq.create.reset-stage
  (:require [clojure.config :as config]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (let [actors (map #(let [[f & params] %]
                       (apply f ctx params))
                    (config/edn-resource "ui_actors.edn"))]
    (doseq [actor actors]
      (stage/add! stage (scene2d/build actor))))
  ctx)
