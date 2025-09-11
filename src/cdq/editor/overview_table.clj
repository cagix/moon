(ns cdq.editor.overview-table
  (:require [cdq.db :as db]
            [cdq.gdx.graphics :as graphics]
            [cdq.editor.property :as property]
            [cdq.utils :refer [pprint-to-str]]))

(def ^:private overview
  {
   :properties/audiovisuals {:columns 10
                             :image-scale 2
                             :sort-by-fn (comp name :property/id)
                             :extra-info-text (constantly "")}

   :properties/creatures {:columns 15
                          :image-scale 1.5
                          :sort-by-fn #(vector (:creature/level %)
                                               (name (:entity/species %))
                                               (name (:property/id %)))
                          :extra-info-text #(str (:creature/level %))}

   :properties/items {:columns 20
                      :image-scale 1.1
                      :sort-by-fn #(vector (name (:item/slot %))
                                           (name (:property/id %)))
                      :extra-info-text (constantly "")}

   :properties/projectiles {:columns 16
                            :image-scale 2
                            :sort-by-fn (comp name :property/id)
                            :extra-info-text (constantly "")}

   :properties/skills {:columns 16
                       :image-scale 2
                       :sort-by-fn (comp name :property/id)
                       :extra-info-text (constantly "")}
   })

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
                image-scale]} (overview property-type)
        properties (db/all-raw db property-type)
        properties (try (sort-by sort-by-fn properties)
                        (catch Throwable t
                          (println"failed to sort:")
                          (clojure.pprint/pprint properties)
                          )
                        )]
    {:actor/type :actor.type/table
     :cell-defaults {:pad 5}
     :rows (for [properties (partition-all columns properties)]
             (for [property properties]
               {:actor
                {:actor/type :actor.type/stack
                 :actors [{:actor/type :actor.type/image-button
                           :drawable/texture-region (graphics/texture-region graphics
                                                                             (property/image property))
                           :on-clicked (fn [_actor ctx]
                                         (clicked-id-fn (:property/id property) ctx))
                           :drawable/scale image-scale
                           :tooltip (pprint-to-str property)}
                          {:actor/type :actor.type/label
                           :label/text (extra-info-text property)
                           :actor/touchable :disabled}]}}))}))
