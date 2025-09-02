(ns cdq.game.render-stage
  (:require [cdq.ui.stage :as stage]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (stage/render! stage ctx))
