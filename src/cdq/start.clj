(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main []
  (let [ctx (-> "ctx.edn" io/resource slurp edn/read-string)]
    (reduce (fn [ctx f]
              (f ctx))
            ctx
            (map #(requiring-resolve (symbol (str "cdq.start." % "/do!")))
                 (:cdq.start (:ctx/config ctx))))))
