(ns cdq.render.ui
  (:require [gdl.ui :as ui]))

(defn do! [{:keys [ctx/stage]
            :as ctx}]
  (ui/act! stage ctx)
  (ui/draw! stage ctx)
  ctx)
