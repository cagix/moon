(ns forge.system
  (:require [clojure.string :as str]))

(defn- component-k->namespace [k]
  (symbol (str "forge." (namespace k) "." (name k))))

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars
          :let [method-var (ns-resolve ns-sym (:name (meta system-var)))]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))
    (when method-var
      (assert (keyword? k))
      (assert (var? method-var) (pr-str method-var))
      (let [system @system-var]
        (when (k (methods system))
          (println "WARNING: Overwriting method" (:name (meta method-var)) "on" k))
        (clojure.lang.MultiFn/.addMethod system k method-var)))))

(defn- install-component [component-systems ns-sym k]
  (require ns-sym)
  (add-methods (:required component-systems) ns-sym k)
  (add-methods (:optional component-systems) ns-sym k :optional? true))

(defn install [systems components]
  (doseq [[k v] components]
    (install-component systems
                       (component-k->namespace k)
                       k)))

(defmacro defsystem
  {:arglists '([name docstring? params?])}
  [name-sym & args]
  (let [docstring (if (string? (first args))
                    (first args))
        params (if (string? (first args))
                 (second args)
                 (first args))
        params (if (nil? params)
                 '[_]
                 params)]
    (when (zero? (count params))
      (throw (IllegalArgumentException. "First argument needs to be component.")))
    (when-let [avar (resolve name-sym)]
      (println "WARNING: Overwriting defsystem:" avar))
    `(defmulti ~(vary-meta name-sym assoc :params (list 'quote params))
       ~(str "[[defsystem]] `" (str params) "`"
             (when docstring (str "\n\n" docstring)))
       (fn [[k#] & _args#]
         k#))))

(defmacro defmethods [k & sys-impls]
  `(do
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)]]
        `(do
          (when (get (methods @~sys-var) ~k)
            (println "WARNING: Overwriting defmethod" ~k "on" ~sys-var))
          (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
            ~@fn-body)))
    ~k))
