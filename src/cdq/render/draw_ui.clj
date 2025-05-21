(ns cdq.render.draw-ui
  (:require [gdl.ui :as ui]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (reset! (.ctx stage) ctx)
  (ui/draw! stage)
  nil)
