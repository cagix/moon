(ns cdq.create.stage
  (:require [cdq.application :as application]
            [clojure.gdx.scene2d.stage :as stage]))

(defn do! [ctx]
  (assoc ctx :ctx/stage (stage/create (:ctx/ui-viewport (:ctx/graphics ctx))
                                      (:ctx/batch       (:ctx/graphics ctx))
                                      application/state)))
