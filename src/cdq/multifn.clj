(ns cdq.multifn
  (:import (clojure.lang MultiFn)))

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var (map requiring-resolve system-vars)
          :let [_ (assert (var? system-var))
                method-sym (symbol (str ns-sym "/" (:name (meta system-var))))
                method-var (resolve method-sym)]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym " - method-sym: " method-sym))
    (when method-var
      (assert (keyword? k))
      (assert (var? method-var) (pr-str method-var))
      (let [system @system-var]
        (when (k (methods system))
          (println "WARNING: Overwriting method" (:name (meta method-var)) "on" k))
        ;(println "MultiFn/.addMethod " system-var  k method-var)
        (MultiFn/.addMethod system k method-var)))))

(defn- install-methods [{:keys [required optional]} ns-sym k]
  (add-methods required ns-sym k)
  (add-methods optional ns-sym k :optional? true))

(defn add-methods! [[multis components]]
  (doseq [[ns-sym k] components]
    (require ns-sym)
    (install-methods multis ns-sym k)))
