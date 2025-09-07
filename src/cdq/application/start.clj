(ns cdq.application.start
  (:require [cdq.application.os-specific-settings :as os-specific-settings]
            [cdq.application.lwjgl :as lwjgl-application]

            cdq.application.context.record
            [clojure.gdx.scenes.scene2d :as scene2d]
            [cdq.ctx])
  (:gen-class))

(extend-type cdq.application.context.record.Context
  clojure.gdx.scenes.scene2d/Context
  (handle-draws! [ctx draws]
    (cdq.ctx/handle-draws! ctx draws)))

(defn -main []
  (os-specific-settings/handle!)
  (lwjgl-application/start!))
