(ns cdq.render.render-stage
  (:require [cdq.ui.stage :as stage]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (stage/set-ctx! stage ctx)
  (stage/act! stage)
  (stage/draw! stage))
