(ns cdq.create.stage
  (:require [clojure.gdx.scene2d.stage :as stage]))

(defn do! [ctx]
  (assoc ctx :ctx/stage (stage/create (:ctx/ui-viewport (:ctx/graphics ctx))
                                      (:ctx/batch       (:ctx/graphics ctx)))))
