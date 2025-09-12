(ns cdq.tx.reset-stage
  (:require [clojure.gdx.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/config
           ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (let [config (:cdq.application.reset-game-state config)
        actors (map #(let [[f params] %]
                       (f ctx params))
                    (:create-ui-actors config))]
    (doseq [actor actors]
      (stage/add! stage (actor/build actor))))
  nil)
