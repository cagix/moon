(ns cdq.ui.editor.widget.one-to-one
  (:require [cdq.db :as db]
            [cdq.g :as g]
            [cdq.stage :as stage]
            [cdq.property :as property]
            [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui]
            [gdl.utils :refer [pprint-to-str]]))

(defn- add-one-to-one-rows [ctx table property-type property-id]
  (let [redo-rows (fn [ctx id]
                    (ui/clear-children! table)
                    (add-one-to-one-rows ctx table property-type id)
                    (ui/pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(when-not property-id
         (ui/text-button "+"
                         (fn [_actor ctx]
                           (let [window (ui/window {:title "Choose"
                                                    :modal? true
                                                    :close-button? true
                                                    :center? true
                                                    :close-on-escape? true})
                                 clicked-id-fn (fn [id ctx]
                                                 (.remove window)
                                                 (redo-rows ctx id))]
                             (ui/add! window (g/property-overview-table ctx property-type clicked-id-fn))
                             (.pack window)
                             (stage/add-actor! ctx window)))))]
      [(when property-id
         (let [property (db/build ctx property-id)
               image-widget (ui/image->widget (property/image property)
                                              {:id property-id})]
           (ui/add-tooltip! image-widget (pprint-to-str property))
           image-widget))]
      [(when property-id
         (ui/text-button "-"
                         (fn [_actor ctx]
                           (redo-rows ctx nil))))]])))

(defmethod widget/create :s/one-to-one [[_ property-type] property-id ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows ctx table property-type property-id)
    table))

(defmethod widget/value :s/one-to-one [_ widget _schemas]
  (->> (ui/children widget)
       (keep ui/user-object)
       first))
