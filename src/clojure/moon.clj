(in-ns 'clojure.core)

(defn bind-root [avar value]
  (.bindRoot avar value))

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
