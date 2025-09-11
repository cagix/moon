(ns cdq.start
  (:require cdq.schema.animation
            cdq.schema.components-ns
            cdq.schema.int
            cdq.schema.map
            cdq.schema.map-optional
            cdq.schema.nat-int
            cdq.schema.number
            cdq.schema.one-to-many
            cdq.schema.one-to-one
            cdq.schema.pos
            cdq.schema.pos-int
            cdq.schema.sound
            cdq.schema.val-max
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main []
  (let [ctx (-> "ctx.edn" io/resource slurp edn/read-string)]
    (reduce (fn [ctx f]
              (f ctx))
            ctx
            (map requiring-resolve (:cdq.start (:ctx/config ctx))))))
