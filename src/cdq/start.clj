(ns cdq.start
  (:require cdq.schema.animation
            cdq.schema.boolean
            cdq.schema.enum
            cdq.schema.image
            cdq.schema.map
            cdq.schema.number
            cdq.schema.one-to-many
            cdq.schema.one-to-one
            cdq.schema.qualified-keyword
            cdq.schema.some
            cdq.schema.sound
            cdq.schema.string
            cdq.schema.val-max
            cdq.schema.vector
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main []
  (let [ctx (-> "ctx.edn" io/resource slurp edn/read-string)]
    (reduce (fn [ctx f]
              (f ctx))
            ctx
            (map requiring-resolve (:cdq.start (:ctx/config ctx))))))
