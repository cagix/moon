(ns cdq.db.schema.map
  (:require [cdq.malli :as m]
            [cdq.db.schema :as schema]
            [cdq.db.schemas :as schemas]
            [cdq.ui.editor.map-widget-table :as map-widget-table]
            [cdq.ui.editor.value-widget :as value-widget]
            [gdl.utils :as utils]
            [clojure.set :as set]))

(defn malli-form [[_ ks] schemas]
  (schemas/create-map-schema schemas ks))

(defn create-value [_ v db]
  (schemas/build-values (:db/schemas db) v db))

(def ^:private property-k-sort-order
  [:property/id
   :property/pretty-name
   :entity/image
   :entity/animation
   :entity/species
   :creature/level
   :entity/body
   :item/slot
   :projectile/speed
   :projectile/max-range
   :projectile/piercing?
   :skill/action-time-modifier-key
   :skill/action-time
   :skill/start-action-sound
   :skill/cost
   :skill/cooldown])

(defn create
  [schema
   m
   {:keys [ctx/db]
    :as ctx}]
  (let [schemas (:db/schemas db)]
    {:actor/type :actor.type/map-widget-table
     :schema schema
     :k->widget (into {}
                      (for [[k v] m]
                        [k (value-widget/build ctx (get schemas k) k v)]))
     :k->optional? #(m/optional? % (schema/malli-form schema schemas))
     :ks-sorted (map first (utils/sort-by-k-order property-k-sort-order m))
     :opt? (seq (set/difference (m/optional-keyset (schema/malli-form schema schemas))
                                (set (keys m))))}))

(defn value [_ table schemas]
  (map-widget-table/get-value table schemas))
