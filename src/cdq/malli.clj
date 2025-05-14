(ns cdq.malli)

(defn map-keys [map-schema]
  (let [[_m _p & ks] map-schema]
    (for [[k m? _schema] ks]
      k)))

(comment
 (= (map-keys
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    [:foo :bar :baz :boz :asdf]))

(defn- map-form-k->properties
  "Given a map schema gives a map of key to key properties (like :optional)."
  [map-schema]
  (let [[_m _p & ks] map-schema]
    (into {} (for [[k m? _schema] ks]
               [k (if (map? m?) m?)]))))

(comment
 (= (map-form-k->properties
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    {:foo nil,
     :bar nil,
     :baz {:optional true},
     :boz {:optional false},
     :asdf {:optional true}}))

(defn optional? [k map-schema]
  (:optional (k (map-form-k->properties map-schema))))

(comment
 (= (optional? :foo
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    nil)

 (= (optional? :baz
               [:map {:closed true}
                [:foo]
                [:bar]
                [:baz {:optional true}]
                [:boz {:optional false}]
                [:asdf {:optional true}]])
    true)

 (= (optional? :asdf
               [:map {:closed true}
                [:foo]
                [:bar]
                [:baz {:optional true}]
                [:boz {:optional false}]
                [:asdf {:optional true}]])
    true)
 )
