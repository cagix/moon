(ns cdq.tx.reset-stage
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.stage :as stage]
            [clojure.symbol :as symbol]))

(defn do!
  [{:keys [ctx/stage
           ctx/ui-actors]
    :as ctx}]
  (stage/clear! stage)
  (let [actors (map #(let [[f params] %]
                       (f ctx params))
                    (-> ui-actors
                        io/resource
                        slurp
                        edn/read-string
                        symbol/require-resolve-symbols))]
    (doseq [actor actors]
      (stage/add! stage (scene2d/build actor))))
  nil)
