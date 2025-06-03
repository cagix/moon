(ns cdq.malli
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]))

(defn schema [form]
  (m/schema form))

(defn validate [schema value]
  (m/validate schema value))

(defn generate [form opts]
  (mg/generate form opts))

(defn validate-humanize [schema value]
  (when-not (validate schema value)
      (throw (ex-info (str (me/humanize (m/explain schema value)))
                      {:value value
                       :schema (m/form schema)}))))

(defn form->validate [form value]
  (let [schema (schema form)]
    (validate-humanize schema value)))

(comment
 (nil? (form->validate [:map {:closed true}
                        [:foo pos?]
                        [:bar pos?]
                        [:baz {:optional true} :some]
                        [:boz {:optional false} :some]
                        [:asdf {:optional true} :some]]
                       {:foo 1
                        :bar 2
                        :boz :a
                        :asdf :b
                        :baz :asdf}))

 (nil? (form->validate [:map {:closed true}
                        [:foo pos?]
                        [:bar pos?]
                        [:baz {:optional true} :some]
                        [:boz {:optional false} :some]
                        [:asdf {:optional true} :some]]
                       {:foo 1
                        :bar 2
                        :boz :a}))

 (require 'clojure.test)
 (clojure.test/is (thrown?
                   clojure.lang.ExceptionInfo
                   (form->validate [:map {:closed true}
                                    [:foo pos?]
                                    [:bar pos?]
                                    [:baz {:optional true} :some]
                                    [:boz {:optional false} :some]
                                    [:asdf {:optional true} :some]]
                                   {:bar 2
                                    :boz :a})))
 )

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

(defn optional-keyset [map-schema]
  (set (filter #(optional? % map-schema)
               (map-keys map-schema))))

(comment
 (= (optional-keyset
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    #{:baz :asdf})
 )
