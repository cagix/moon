(ns cdq.start
  (:require cdq.schema.one-to-one
            cdq.schema.one-to-many
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main []
  (let [ctx (-> "ctx.edn" io/resource slurp edn/read-string)]
    (reduce (fn [ctx f]
              (f ctx))
            ctx
            (map requiring-resolve (:cdq.start (:ctx/config ctx))))))
