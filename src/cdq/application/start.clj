(ns cdq.application.start
  (:require cdq.application.context.record
            cdq.application
            cdq.application.os-specific-settings
            cdq.create.colors
            cdq.application.lwjgl
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

; TODO requiring-resolve is the problem !! obscures abstraction layers/dependencies
; creates 'fake order'

(defn -main []
  (let [ctx (-> "ctx.edn"
                io/resource
                slurp
                edn/read-string)]
    (reduce (fn [ctx f]
              (if-let [new-ctx (f ctx)]
                new-ctx
                ctx))
            ctx
            [cdq.application.context.record/create
             cdq.application/self-reference
             cdq.application.os-specific-settings/handle!
             cdq.create.colors/do!
             cdq.application.lwjgl/start!])))
