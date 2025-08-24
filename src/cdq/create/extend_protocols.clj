(ns cdq.create.extend-protocols
  (:require [cdq.string :as str]))

(defn- create-method-map [protocol namespace]
  (let [{:keys [sigs
                var]} protocol]
    (into {}
          (for [[k signature] sigs]
            [k (requiring-resolve (symbol (str (str namespace ".")
                                               (str/pascal->kebab (name (symbol var)))
                                               "/"
                                               (:name signature))))]))))

; TODO do out of 'create'
(defn do! [ctx namespace]
  (let [protocols (map (comp deref second)
                       (filter #(:sigs @(% 1)) (ns-publics namespace)))]
    (doseq [protocol protocols]
      ;(println)
      ;(println "Extending protocol: " (:on protocol))
      ;(println "With method map: ")
      ;(clojure.pprint/pprint (create-method-map protocol namespace))
      (extend (class ctx)
        protocol
        (create-method-map protocol namespace))))
  ctx)
