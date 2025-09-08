(ns cdq.application.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

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
            (map requiring-resolve '[cdq.application.context.record/create
                                     cdq.application/self-reference
                                     cdq.application.os-specific-settings/handle!
                                     cdq.create.colors/do!
                                     cdq.application.lwjgl/start!]))))
