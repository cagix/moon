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
  {:actor/type :actor.type/table
   :cell-defaults {:pad 5}
   :rows (let [{:keys [sort-by-fn
                       extra-info-text
                       columns
                       image-scale]} (cdq.application/property-overview property-type)
               properties (db/all-raw db property-type)
               properties (sort-by sort-by-fn properties)
               table-cells (for [property properties]
                             {:texture-region (graphics/texture-region graphics (property/image property))
                              :on-clicked (fn [_actor ctx]
                                            (clicked-id-fn (:property/id property) ctx))
                              :tooltip (pprint-to-str property)
                              :extra-info-text (extra-info-text property)})
               rows (partition-all columns table-cells)]
           (for [row rows]
             (for [{:keys [texture-region
                           on-clicked
                           tooltip
                           extra-info-text]} row]
               {:actor {:actor/type :actor.type/stack
                        :actors [{:actor/type :actor.type/image-button
                                  :drawable/texture-region texture-region
                                  :on-clicked on-clicked
                                  :drawable/scale image-scale
                                  :tooltip tooltip}
                                 {:actor/type :actor.type/label
                                  :label/text extra-info-text
                                  :actor/touchable :disabled}]}})))})
