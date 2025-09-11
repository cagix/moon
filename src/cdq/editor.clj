(ns cdq.editor
  (:require [cdq.application]
            [cdq.db :as db]
            [cdq.gdx.graphics :as graphics]
            [cdq.editor.property :as property]
            [cdq.editor.overview-table :as overview-table]
            [cdq.utils :refer [pprint-to-str]]))

(defn overview-table
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
                 :tooltip (pprint-to-str property)
                 :extra-info-text (extra-info-text property)}))
         (partition-all columns)
         (overview-table/create image-scale))))
