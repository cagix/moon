(ns cdq.application.extend-scene2d
  (:require [cdq.application.context-record]
            [cdq.ctx :as ctx]
            [clojure.gdx.scenes.scene2d]))

(extend-type cdq.application.context_record.Context
  clojure.gdx.scenes.scene2d/Context
  (handle-draws! [ctx draws]
    (cdq.ctx/handle-draws! ctx draws)))
