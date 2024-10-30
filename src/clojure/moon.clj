(in-ns 'clojure.core)

(require 'clojure.pprint)

(defn pexpand-macro [form]
  (set! *print-level* nil)
  (clojure.pprint/pprint (macroexpand-1 form)))

(defn bind-root [avar value]
  (.bindRoot avar value))

(def defc-ns-docs? true)

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

(def component-attrs {})

(defn defc*
  [k attr-map]
  (when (component-attrs k)
    (println "WARNING: Overwriting defc" k "attr-map"))
  (alter-var-root #'component-attrs assoc k attr-map))

; FIXME plain ones
; (defc-ns-sym :ui)
; => moon..ui
(defn defc-ns-sym [k]
  (symbol (str "moon." (namespace k) "." (name k))))

(defn defc-check-ns [k]
  (when-not (= (ns-name *ns*) (defc-ns-sym k))
    (println (ns-name *ns*) ":" k)))

(def print-ns-mismatch? true)

(defmacro defc [k & sys-impls]
  (let [attr-map? (not (list? (first sys-impls)))
        attr-map  (if attr-map? (first sys-impls) {})
        sys-impls (if attr-map? (rest sys-impls) sys-impls)
        let-bindings (:let attr-map)
        attr-map (dissoc attr-map :let)]
    `(do
      (when print-ns-mismatch?
        (defc-check-ns ~k))
      (when ~attr-map?
        (defc* ~k ~attr-map))
      (when defc-ns-docs?
        (alter-meta! *ns* #(update % :doc str "\n* defc `" ~k "`"
                                   (when (:schema ~attr-map)
                                     (str " schema: `" (:schema ~attr-map) "`"))
                                   (when (:doc ~attr-map)
                                     (str "\n\n" (:doc ~attr-map))))))
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
             (alter-var-root #'component-attrs assoc-in [~k :params ~(name (symbol sys-var))] (quote ~fn-params))
             (when (get (methods @~sys-var) ~k)
               (println "WARNING: Overwriting defc" ~k "on" ~sys-var))
             (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
               [& params#]
               (let [~(if let-bindings let-bindings '_) (get (first params#) 1) ; get because maybe component is just [:foo] without v.
                     ~fn-params params#]
                 ~@fn-exprs)))))
      ~k)))
