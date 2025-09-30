(ns cdq.ctx.create-vis-ui
  (:require [clojure.scene2d.vis-ui :as vis-ui]))

(def params {:skin-scale :x1})

(defn do! [ctx]
  (assoc ctx :ctx/vis-ui (vis-ui/load! params)))
