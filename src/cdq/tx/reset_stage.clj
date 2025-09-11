(ns cdq.tx.reset-stage
  (:require [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/config
           ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (let [config (:cdq.application.reset-game-state config)
        actors (map #(let [[f params] %]
                       ((requiring-resolve f) ctx params))
                    (:create-ui-actors config))]
    (doseq [actor actors]
      (stage/add! stage (actor/build actor))))
  nil)
