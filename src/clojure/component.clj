(ns clojure.component)

(defmacro defsystem
  {:arglists '([name docstring?])}
  [name-sym & args]
  (let [docstring (if (string? (first args))
                    (first args))]
    `(defmulti ~name-sym
       ~(str "[[defsystem]]" (when docstring (str "\n\n" docstring)))
       (fn [[k#] & args#]
         k#))))

(def overwrite-warnings? false)

(defmacro defcomponent [k & sys-impls]
  `(do
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)]]
        `(do
          (when (and overwrite-warnings?
                     (get (methods @~sys-var) ~k))
            (println "warning: overwriting defmethod" ~k "on" ~sys-var))
          (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
            ~@fn-body)))
    ~k))
