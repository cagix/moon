(ns cdq.ui.editor.overview-table
  (:require [cdq.db :as db]
            [cdq.editor]
            [cdq.graphics :as graphics]
            [cdq.ui.editor.property :as property]
            [cdq.string :as string]))

(def ^:private property-type->overview-table-props
  {:properties/audiovisuals {:columns 10
                             :image-scale 2
                             :sort-by-fn (comp name :property/id)
                             :extra-info-text (constantly "")}
   :properties/creatures    {:columns 15
                             :image-scale 1.5
                             :sort-by-fn #(vector (:creature/level %)
                                                  (name (:entity/species %))
                                                  (name (:property/id %)))
                             :extra-info-text #(str (:creature/level %))}
   :properties/items        {:columns 20
                             :image-scale 1.1
                             :sort-by-fn #(vector (name (:item/slot %))
                                                  (name (:property/id %)))
                             :extra-info-text (constantly "")}
   :properties/projectiles  {:columns 16
                             :image-scale 2
                             :sort-by-fn (comp name :property/id)
                             :extra-info-text (constantly "")}
   :properties/skills       {:columns 16
                             :image-scale 2
                             :sort-by-fn (comp name :property/id)
                             :extra-info-text (constantly "")}})

(defn- create* [image-scale rows]
  (for [row rows]
    (for [{:keys [texture-region
                  on-clicked
                  tooltip
                  extra-info-text]} row]
      {:actor {:actor/type :actor.type/stack
               :group/actors [{:actor/type :actor.type/image-button
                               :drawable/texture-region texture-region
                               :on-clicked on-clicked
                               :drawable/scale image-scale
                               :tooltip tooltip}
                              {:actor/type :actor.type/label
                               :label/text extra-info-text
                               :actor/touchable :disabled}]}})))

(defn- create
  [{:keys [ctx/db
           ctx/graphics]}
   property-type
   clicked-id-fn]
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image-scale]} (get property-type->overview-table-props property-type)]
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

(defn extend-ctx [ctx]
  (extend-type (class ctx)
    cdq.editor/Editor
    (overview-table-rows [ctx property-type clicked-id-fn]
      (create ctx property-type clicked-id-fn)))
  ctx)
