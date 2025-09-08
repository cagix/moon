(ns cdq.start.extend-scene2d
  (:require [cdq.graphics :as graphics]
            [clojure.gdx.scenes.scene2d]))

(defn do! [ctx]
  (extend-type (class ctx)
    clojure.gdx.scenes.scene2d/Context
    (handle-draws! [ctx draws]
      (graphics/handle-draws! ctx draws)))
  ctx)
