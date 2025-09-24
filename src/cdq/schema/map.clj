(ns cdq.schema.map
  (:require [cdq.malli :as m]
            [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.ui.editor.window :as editor-window]
            [cdq.ui.editor.value-widget :as value-widget]
            [clojure.utils :as utils]
            [clojure.set :as set]))

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
  (editor-window/get-value table schemas))
