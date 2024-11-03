(in-ns 'clojure.core)

(defn bind-root [avar value]
  (.bindRoot avar value))

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

(defn defc-ns-sym [k]
  (symbol (str "moon." (namespace k) "." (name k))))

(defn defc-check-ns [k]
  (when-not (= (ns-name *ns*) (defc-ns-sym k))
    (println (ns-name *ns*) ":" k)))

(defmacro defmethods [k & sys-impls]
  (assert (keyword? k) (pr-str k))
  `(do
    (defc-check-ns ~k)
    (alter-meta! *ns* #(update % :doc str "\n* defmethods `" ~k "`"))
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)]]
        `(do
          (when (get (methods @~sys-var) ~k)
            (println "WARNING: Overwriting defmethod" ~k "on" ~sys-var))
          (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
            ~@fn-body)))
    ~k))
