(ns cdq.editor.overview-table
  (:require [cdq.application]
            [cdq.db :as db]
            [cdq.gdx.graphics :as graphics]
            [cdq.editor.property :as property]
            [cdq.utils :refer [pprint-to-str]]))

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
                image-scale]} (cdq.application/property-overview property-type)
        properties (db/all-raw db property-type)
        properties (sort-by sort-by-fn properties)]
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
