(ns cdq.create.ctx-extenders
  (:require [gdl.utils :as utils]))

(defn- create-method-map [protocol]
  (let [{:keys [sigs
                var]} protocol]
    (into {}
          (for [[k signature] sigs]
            [k (requiring-resolve (symbol (str "clojure.ctx."
                                               (utils/pascal->kebab (name (symbol var)))
                                               "/"
                                               (:name signature))))]))))

(defn do! [ctx]
  (let [protocols (map (comp deref second)
                       (filter #(:sigs @(% 1)) (ns-publics 'clojure.ctx)))]
    (doseq [protocol protocols]
      ;(println)
      ;(println "Extending protocol: " (:on protocol))
      ;(println "With method map: ")
      ;(clojure.pprint/pprint (create-method-map protocol))
      (extend (class ctx)
        protocol
        (create-method-map protocol))))
  ctx)


