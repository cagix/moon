(ns cdq.render.update-ui
  (:require [gdl.ui :as ui]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (reset! (.ctx stage) ctx)
  (ui/act! stage)
  nil)
