(ns cdq.application.start
  (:require [cdq.application.os-specific-settings :as os-specific-settings]
            [cdq.application.lwjgl :as lwjgl-application]
            cdq.application.context.record
            [clojure.gdx.scenes.scene2d :as scene2d]
            [cdq.ctx]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

(extend-type cdq.application.context.record.Context
  clojure.gdx.scenes.scene2d/Context
  (handle-draws! [ctx draws]
    (cdq.ctx/handle-draws! ctx draws)))

(defn -main []
  (let [ctx (-> "ctx.edn"
                io/resource
                slurp
                edn/read-string)]
    ; walk & require-resolve !
    ; fix all require-resolve !
    (os-specific-settings/handle! ctx)
    (lwjgl-application/start! ctx)))
