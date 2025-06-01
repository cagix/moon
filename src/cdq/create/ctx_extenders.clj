(ns cdq.create.ctx-extenders
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.interaction-state]
            [clojure.string :as str]))

(defn- pascal->kebab [s]
  (-> s
      (str/replace #"([a-z])([A-Z])" "$1-$2")
      (str/replace #"([A-Z]+)([A-Z][a-z])" "$1-$2")
      (str/lower-case)))

(defn- create-method-map [protocol]
  (let [{:keys [sigs
                var]} protocol]
    (into {}
          (for [[k signature] sigs]
            [k (requiring-resolve (symbol (str "cdq.ctx."
                                               (pascal->kebab (name (symbol var)))
                                               "/"
                                               (:name signature))))]))))

(defn do! [ctx]
  (let [protocols (map (comp deref second)
                       (filter #(:sigs @(% 1)) (ns-publics 'cdq.ctx)))]
    (doseq [protocol protocols]
      (println)
      (println "Extending protocol: " (:on protocol))
      (println "With method map: ")
      (clojure.pprint/pprint (create-method-map protocol))
      (extend (class ctx)
        protocol
        (create-method-map protocol))))
  ctx)


