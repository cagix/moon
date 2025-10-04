(ns cdq.ctx.create.vis-ui
  (:require [clojure.scene2d.vis-ui :as vis-ui]))

(defn do! [ctx params]
  (assoc ctx :ctx/vis-ui (vis-ui/load! params)))
