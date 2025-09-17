(ns cdq.schema.map
  (:require [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.ui.editor.window :as editor-window]
            [cdq.malli :as m]
            [clojure.utils :as utils]
            [clojure.set :as set]))

(defn malli-form [[_ ks] schemas]
  (schemas/create-map-schema schemas ks))

(defn create-value [_ v db]
  (schemas/build-values (:schemas db) v db))

(defn create
  [schema
   m
   {:keys [ctx/db
           ctx/editor]
    :as ctx}]
  (let [schemas (:schemas db)]
    (editor-window/map-widget-table
     {:schema schema
      :k->widget (into {}
                       (for [[k v] m]
                         [k (editor-window/build-widget ctx (get schemas k) k v)]))
      :k->optional? #(m/optional? % (schema/malli-form schema schemas))
      :ks-sorted (map first (utils/sort-by-k-order (:editor/property-k-sort-order editor) m))
      :opt? (seq (set/difference (m/optional-keyset (schema/malli-form schema schemas))
                                 (set (keys m))))})))

(defn value [_ table schemas]
  (editor-window/map-widget-property-values table schemas))
