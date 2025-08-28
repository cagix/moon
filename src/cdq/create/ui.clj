(ns cdq.create.ui
  (:require [cdq.input :as input]
            [gdx.ui :as ui]))

(defn do! [graphics input params]
  (ui/load! params)
  (let [stage (ui/stage (:ui-viewport graphics)
                        (:batch       graphics))]
    (input/set-processor! input stage)
    stage))
