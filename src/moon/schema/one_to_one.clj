(ns moon.schema.one-to-one
  (:require [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.component :as component]
            [moon.db :as db]
            [moon.property :as property]
            [moon.schema :as schema]
            [moon.stage :as stage] ))

(defmethod schema/form :s/one-to-one [[_ property-type]]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])

(defmethod schema/edn->value :s/one-to-one [_ property-id]
  (db/get property-id))

(defn- add-one-to-one-rows [table property-type property-id]
  (let [redo-rows (fn [id]
                    (ui/clear-children! table)
                    (add-one-to-one-rows table property-type id)
                    (ui/pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(when-not property-id
         (ui/text-button "+"
                         (fn []
                           (let [window (ui/window {:title "Choose"
                                                    :modal? true
                                                    :close-button? true
                                                    :center? true
                                                    :close-on-escape? true})
                                 clicked-id-fn (fn [id]
                                                 (a/remove! window)
                                                 (redo-rows id))]
                             (.add window (component/create
                                           [:widgets/properties-overview
                                            property-type
                                            clicked-id-fn]))
                             (.pack window)
                             (stage/add! window)))))]
      [(when property-id
         (let [property (db/get property-id)
               image-widget (ui/image->widget (property/->image property) {:id property-id})]
           (ui/add-tooltip! image-widget #(component/->info property))
           image-widget))]
      [(when property-id
         (ui/text-button "-" #(redo-rows nil)))]])))

(defmethod schema/widget :s/one-to-one [[_ property-type] property-id]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows table property-type property-id)
    table))

(defmethod schema/widget-value :s/one-to-one [_ widget]
  (->> (ui/children widget)
       (keep a/id)
       first))
