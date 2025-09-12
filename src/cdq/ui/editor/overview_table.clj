(ns cdq.ui.editor.overview-table
  (:require [cdq.application]
            [cdq.ctx.db :as db]
            [cdq.gdx.graphics :as graphics]
            [cdq.ui.editor.property :as property]
            [cdq.string :as string]))

(defn- create* [image-scale rows]
  {:actor/type :actor.type/table
   :cell-defaults {:pad 5}
   :rows (for [row rows]
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
                                :actor/touchable :disabled}]}}))})

(defn create
  [{:keys [ctx/db
           ctx/graphics]}
   property-type
   clicked-id-fn]
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image-scale]} (cdq.application/property-overview property-type)]
    (->> (db/all-raw db property-type)
         (sort-by sort-by-fn)
         (map (fn [property]
                {:texture-region (graphics/texture-region graphics (property/image property))
                 :on-clicked (fn [_actor ctx]
                               (clicked-id-fn (:property/id property) ctx))
                 :tooltip (string/pprint-to-str property)
                 :extra-info-text (extra-info-text property)}))
         (partition-all columns)
         (create* image-scale))))
