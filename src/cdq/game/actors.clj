(ns cdq.game.actors
  (:require [clojure.ui.stage :as stage]))

(defn do! [{:keys [ctx/config
                   ctx/stage] :as ctx}]
  (stage/clear! stage)
  (doseq [create-actor (:actor-create-fns config)]
    (stage/add! stage (create-actor ctx)))
  ctx)
