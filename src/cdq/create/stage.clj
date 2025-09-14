(ns cdq.create.stage
  (:require [clojure.gdx.scene2d.ctx-stage :as ctx-stage]))

(defn do! [ctx]
  (assoc ctx :ctx/stage (ctx-stage/create (:ctx/ui-viewport (:ctx/graphics ctx))
                                          (:ctx/batch       (:ctx/graphics ctx)))))
