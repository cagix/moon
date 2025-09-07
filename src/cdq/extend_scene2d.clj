(ns cdq.extend-scene2d
  (:require [clojure.gdx.scenes.scene2d :as scene2d]
            [cdq.ctx]))

(defn do! [ctx]
  (extend-type (class ctx)
    clojure.gdx.scenes.scene2d/Context
    (handle-draws! [ctx draws]
      (cdq.ctx/handle-draws! ctx draws))))
