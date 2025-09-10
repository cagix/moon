(ns cdq.create.stage
  (:require [clojure.gdx.scenes.scene2d :as scene2d]))

(defn do! [ctx]
  (assoc ctx :ctx/stage (scene2d/stage (:ctx/ui-viewport (:ctx/graphics ctx))
                                       (:ctx/batch       (:ctx/graphics ctx)))))
