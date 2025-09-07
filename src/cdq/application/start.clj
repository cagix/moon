(ns cdq.application.start
  (:require [cdq.application]
            ;;
            cdq.application.context.record
            [clojure.gdx.scenes.scene2d :as scene2d]
            [cdq.ctx]
            ;;
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
    (reduce (fn [ctx f]
              (if-let [new-ctx (f ctx)]
                new-ctx
                ctx))
            (assoc ctx :ctx/application-state cdq.application/state)
            (map requiring-resolve (:ctx/initial-pipeline ctx)))))
