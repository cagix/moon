(in-ns 'clojure.core)

(def component-attrs {})

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
  (let [let-bindings (if (not (list? (first sys-impls)))
                       (:let (first sys-impls)))
        sys-impls (if let-bindings
                    (rest sys-impls)
                    sys-impls)]
    `(do
      (defc-check-ns ~k)
      (alter-meta! *ns* #(update % :doc str "\n* defmethods `" ~k "`"))
      ~@(for [[sys & fn-body] sys-impls
              :let [sys-var (resolve sys)
                    sys-params (:params (meta sys-var))
                    fn-params (first fn-body)
                    fn-exprs (rest fn-body)]]
          (do
           (when-not sys-var
             (throw (IllegalArgumentException. (str sys " does not exist."))))
           (when-not (= (count sys-params) (count fn-params)) ; defmethods do not check this, that's why we check it here.
             (throw (IllegalArgumentException.
                     (str sys-var " requires " (count sys-params) " args: " sys-params "."
                          " Given " (count fn-params)  " args: " fn-params))))
           `(do
             (assert (keyword? ~k) (pr-str ~k))
             (when (get (methods @~sys-var) ~k)
               (println "WARNING: Overwriting defmethod" ~k "on" ~sys-var))
             (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
               [& params#]
               (let [~(if let-bindings let-bindings '_) (get (first params#) 1) ; get because maybe component is just [:foo] without v.
                     ~fn-params params#]
                 ~@fn-exprs)))))
      ~k)))
