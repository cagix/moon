(ns cdq.application.create.editor
  (:require [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.graphics :as graphics]
            [gdl.scene2d :as scene2d]))

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

(defn- overview-table-rows* [image-scale rows]
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

(defn- overview-table-rows
  [db
   graphics
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
                 :on-clicked (fn [actor ctx]
                               (clicked-id-fn actor (:property/id property) ctx))
                 :tooltip (property/tooltip property)
                 :extra-info-text (extra-info-text property)}))
         (partition-all columns)
         (overview-table-rows* image-scale))))

(defmethod scene2d/build :actor.type/editor-overview-window
  [{:keys [db
           graphics
           property-type
           clicked-id-fn]}]
  (scene2d/build
   {:actor/type :actor.type/window
    :title "Edit"
    :modal? true
    :close-button? true
    :center? true
    :close-on-escape? true
    :pack? true
    :rows (overview-table-rows db
                               graphics
                               property-type
                               clicked-id-fn)}))

(defn do! [ctx]
  ctx)
