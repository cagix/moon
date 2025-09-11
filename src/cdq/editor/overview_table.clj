(ns cdq.editor.overview-table
  (:require [cdq.db :as db]
            [cdq.gdx.graphics :as graphics]
            [cdq.editor.property :as property]
            [cdq.utils :refer [pprint-to-str]]))

; FIXME not refreshed after changes in properties

(def ^:private overview {:properties/audiovisuals {:columns 10
                                                   :image/scale 2}
                         :properties/creatures {:columns 15
                                                :image/scale 1.5
                                                :sort-by-fn #(vector (:creature/level %)
                                                                     (name (:entity/species %))
                                                                     (name (:property/id %)))
                                                :extra-info-text #(str (:creature/level %))}
                         :properties/items {:columns 20
                                            :image/scale 1.1
                                            :sort-by-fn #(vector (name (:item/slot %))
                                                                 (name (:property/id %)))}
                         :properties/projectiles {:columns 16
                                                  :image/scale 2}
                         :properties/skills {:columns 16
                                             :image/scale 2}})

(defn create
  [{:keys [ctx/db
           ctx/graphics]}
   property-type
   clicked-id-fn]
  (assert (contains? overview property-type)
          (pr-str property-type))
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (overview property-type)
        properties (db/all-raw db property-type)
        properties (if sort-by-fn
                     (sort-by sort-by-fn properties)
                     properties)]
    {:actor/type :actor.type/table
     :cell-defaults {:pad 5}
     :rows (for [properties (partition-all columns properties)]
             (for [{:keys [property/id] :as property} properties
                   :let [on-clicked (fn [_actor ctx]
                                      (clicked-id-fn id ctx))
                         tooltip-text (pprint-to-str property)]]
               {:actor
                {:actor/type :actor.type/stack
                 :actors [(if-let [texture-region (when-let [image (property/image property)]
                                                    (graphics/texture-region graphics image))]
                            {:actor/type :actor.type/image-button
                             :drawable/texture-region texture-region
                             :on-clicked on-clicked
                             :drawable/scale scale
                             :tooltip tooltip-text}
                            {:actor/type :actor.type/text-button
                             :text (name id)
                             :on-clicked on-clicked
                             :tooltip tooltip-text})
                          {:actor/type :actor.type/label
                           :label/text (or (and extra-info-text
                                                (extra-info-text property))
                                           "")
                           :actor/touchable :disabled}]}}))}))
