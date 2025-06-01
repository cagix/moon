(ns cdq.create.input)

(defn protocol? [object]
  (:sigs object))

(defn create-method-map
  "Creates a method name for all functions in `protocol`, for each function calling the implementation
  with same name in `impl-namespace`, by using `(ctx-k ctx)` as first parameter."
  [& {:keys [protocol impl-namespace ctx-k]}]
  {:pre [(keyword? ctx-k)
         (symbol? impl-namespace)
         (protocol? protocol)]}
  (into {}
        (for [[k function-signature] (:sigs protocol)
              :let [params (first (:arglists function-signature))
                    impl-function-name (symbol (str impl-namespace) (str (:name function-signature)))]]
          [k `(fn ~params (~impl-function-name (~ctx-k ~(first params)) ~@(rest params)))])))

(comment

 ; Example:

 (clojure.pprint/pprint
  (create-method-map :protocol g/Input
                     :impl-namespace 'clojure.gdx.input
                     :ctx-k :ctx/input))

 {:button-just-pressed?
  (clojure.core/fn
    [_ button]
    (clojure.gdx.input/button-just-pressed? (:ctx/input _) button)),
  :key-pressed?
  (clojure.core/fn
    [_ key]
    (clojure.gdx.input/key-pressed? (:ctx/input _) key)),
  :key-just-pressed?
  (clojure.core/fn
    [_ key]
    (clojure.gdx.input/key-just-pressed? (:ctx/input _) key)),
  :mouse-position
  (clojure.core/fn
    [_]
    (clojure.gdx.input/mouse-position (:ctx/input _)))}

 ((eval (:mouse-position (create-method-map :protocol g/Input
                                            :impl-namespace 'clojure.gdx.input
                                            :ctx-k :ctx/input)))
  {:ctx/input Gdx/input})
 [0 0]

 ((:mouse-position (update-vals (create-method-map :protocol g/Input
                                                   :impl-namespace 'clojure.gdx.input
                                                   :ctx-k :ctx/input)
                                eval))
  {:ctx/input Gdx/input})
 [0 0]
 )

(defn extend-context [ctx {:keys [protocol impl-namespace ctx-k create-k-value]
                           :as opts}]
  (require impl-namespace)
  ;(println "extend-context opts:")
  ;(clojure.pprint/pprint opts)
  (extend (class ctx)
    protocol
    (let [method-map (create-method-map :protocol protocol
                                        :impl-namespace impl-namespace
                                        :ctx-k ctx-k)]
      ;(println "method-map:")
      ;(clojure.pprint/pprint method-map)
      (update-vals method-map eval)))
  (assoc ctx ctx-k ((requiring-resolve create-k-value))))

(defn do! [ctx]
  (extend-context ctx
                  {:protocol cdq.g/Input
                   :impl-namespace 'clojure.gdx.input
                   :ctx-k :ctx/input
                   :create-k-value 'clojure.gdx/input}))
