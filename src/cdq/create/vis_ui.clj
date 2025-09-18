(ns cdq.create.vis-ui
  (:require [com.kotcrab.vis.ui.vis-ui :as vis-ui]))

(defn do! [ctx params]
  (assoc ctx :ctx/vis-ui (vis-ui/load! params)))
