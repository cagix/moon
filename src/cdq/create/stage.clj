(ns cdq.create.stage
  (:require [gdl.input :as input]
            [gdl.ui :as ui]))

(defn do! [{:keys [ctx/config
                   ctx/graphics
                   ctx/input
                   ctx/ui-viewport]
            :as ctx}]
  (ui/load! (:ui config))
  (let [stage (ui/stage (:java-object ui-viewport)
                        (:batch graphics))]
    (input/set-processor! input stage)
    (assoc ctx :ctx/stage stage)))
