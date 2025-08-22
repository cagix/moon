(ns cdq.render.stage
  (:require [gdl.ui.stage :as stage]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (stage/render! stage ctx))
