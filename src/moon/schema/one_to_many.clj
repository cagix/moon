(ns ^:no-doc moon.schema.one-to-many
  (:require [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.db :as db]
            [moon.info :as info]
            [moon.property :as property]
            [moon.schema :as schema]
            [moon.widgets.properties-overview :as properties-overview]))

(defmethod schema/form :s/one-to-many [[_ property-type]]
  [:set [:qualified-keyword {:namespace (property/type->id-namespace property-type)}]])

(defmethod schema/edn->value :s/one-to-many [_ property-ids]
  (set (map db/get property-ids)))

(defn- add-one-to-many-rows [table property-type property-ids]
  (let [redo-rows (fn [property-ids]
                    (ui/clear-children! table)
                    (add-one-to-many-rows table property-type property-ids)
                    (ui/pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(ui/text-button "+"
                       (fn []
                         (let [window (ui/window {:title "Choose"
                                                  :modal? true
                                                  :close-button? true
                                                  :center? true
                                                  :close-on-escape? true})
                               clicked-id-fn (fn [id]
                                               (a/remove! window)
                                               (redo-rows (conj property-ids id)))]
                           (.add window (properties-overview/table property-type clicked-id-fn))
                           (.pack window)
                           (stage/add! window))))]
      (for [property-id property-ids]
        (let [property (db/get property-id)
              image-widget (ui/image->widget (property/->image property)
                                             {:id property-id})]
          (ui/add-tooltip! image-widget #(info/text property))))
      (for [id property-ids]
        (ui/text-button "-" #(redo-rows (disj property-ids id))))])))

(defmethod schema/widget :s/one-to-many [[_ property-type] property-ids]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows table property-type property-ids)
    table))

(defmethod schema/widget-value :s/one-to-many [_ widget]
  (->> (ui/children widget)
       (keep a/id)
       set))
