(ns cdq.application.create.vis-ui
  (:require [clojure.scene2d.vis-ui :as vis-ui]))

(defn do! [ctx]
  (assoc ctx :ctx/vis-ui (vis-ui/load! {:skin-scale :x1})))
