(ns cdq.ui.editor.widget.one-to-one
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.info :as info]
            [cdq.property :as property]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [pprint-to-str]]
            [gdl.ui :as ui]))

(defn- add-one-to-one-rows [table property-type property-id]
  (let [redo-rows (fn [id]
                    (ui/clear-children! table)
                    (add-one-to-one-rows table property-type id)
                    (ui/pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(when-not property-id
         (ui/text-button "+"
                         (fn [_actor {:keys [ctx/db
                                             ctx/stage]}]
                           (let [window (ui/window {:title "Choose"
                                                    :modal? true
                                                    :close-button? true
                                                    :center? true
                                                    :close-on-escape? true})
                                 clicked-id-fn (fn [id _ctx]
                                                 (.remove window)
                                                 (redo-rows id))]
                             (ui/add! window (overview-table/create db property-type clicked-id-fn))
                             (.pack window)
                             (ui/add! stage window)))))]
      [(when property-id
         (let [property (db/build ctx/db property-id (ctx/make-map))
               image-widget (ui/image->widget (property/image property)
                                              {:id property-id})]
           (ui/add-tooltip! image-widget (pprint-to-str property))
           image-widget))]
      [(when property-id
         (ui/text-button "-"
                         (fn [_actor _ctx]
                           (redo-rows nil))))]])))

(defmethod widget/create :s/one-to-one [[_ property-type] property-id _ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows table property-type property-id)
    table))

(defmethod widget/value :s/one-to-one [_ widget]
  (->> (ui/children widget)
       (keep ui/user-object)
       first))
