(ns cdq.editor.overview-table
  (:require [cdq.db :as db]
            [cdq.image :as image]
            [cdq.property :as property]
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
                                            :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                                                   (name slot)
                                                                   "")
                                                                 (name (:property/id %)))}
                         :properties/projectiles {:columns 16
                                                  :image/scale 2}
                         :properties/skills {:columns 16
                                             :image/scale 2}
                         :properties/worlds {:columns 10}
                         :properties/player-dead {:columns 1}
                         :properties/player-idle {:columns 1}
                         :properties/player-item-on-cursor {:columns 1}})

(defn- create-item-stack
  [{:keys [tooltip-text
           label-text
           text-button-text
           texture-region
           scale
           on-clicked]}]
  {:actor/type :actor.type/stack
   :actors [(if texture-region
              {:actor/type :actor.type/image-button
               :drawable/texture-region texture-region
               :on-clicked on-clicked
               :drawable/scale scale
               :tooltip tooltip-text}
              {:actor/type :actor.type/text-button
               :text text-button-text
               :on-clicked on-clicked
               :tooltip tooltip-text})
            {:actor/type :actor.type/label
             :label/text label-text
             :actor/touchable :disabled}]})

(defn create
  [{:keys [ctx/db
           ctx/textures]}
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
             (for [{:keys [property/id] :as property} properties]
               {:actor (create-item-stack
                        {:tooltip-text (pprint-to-str property)
                         :label-text (or (and extra-info-text
                                              (extra-info-text property))
                                         "")
                         :text-button-text (name id)
                         :texture-region (when-let [image (property/image property)]
                                           (image/texture-region image textures))
                         :scale scale
                         :on-clicked (fn [_actor ctx]
                                       (clicked-id-fn id ctx))})}))}))
