(ns cdq.start.extend-scene2d
  (:require [cdq.ctx :as ctx]
            [clojure.gdx.scenes.scene2d]))

(defn do! [ctx]
  (extend-type (class ctx)
    clojure.gdx.scenes.scene2d/Context
    (handle-draws! [ctx draws]
      (cdq.ctx/handle-draws! ctx draws)))
  ctx)
