(ns cdq.ui.editor.widget.one-to-many
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.info :as info]
            [cdq.property :as property]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [pprint-to-str]]
            [gdl.ui :as ui]))

(defn- add-one-to-many-rows [table property-type property-ids]
  (let [redo-rows (fn [property-ids]
                    (ui/clear-children! table)
                    (add-one-to-many-rows table property-type property-ids)
                    (ui/pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(ui/text-button "+"
                       (fn [_actor]
                         (let [window (ui/window {:title "Choose"
                                                  :modal? true
                                                  :close-button? true
                                                  :center? true
                                                  :close-on-escape? true})
                               clicked-id-fn (fn [id]
                                               (.remove window)
                                               (redo-rows (conj property-ids id)))]
                           (ui/add! window (overview-table/create ctx/db property-type clicked-id-fn))
                           (.pack window)
                           (ui/add! ctx/stage window))))]
      (for [property-id property-ids]
        (let [property (db/build ctx/db property-id)
              image-widget (ui/image->widget (property/image property)
                                             {:id property-id})]
          (ui/add-tooltip! image-widget (pprint-to-str property))))
      (for [id property-ids]
        (ui/text-button "-"
                        (fn [_actor]
                          (redo-rows (disj property-ids id)))))])))

(defmethod widget/create :s/one-to-many [[_ property-type] property-ids]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows table property-type property-ids)
    table))

(defmethod widget/value :s/one-to-many [_ widget]
  (->> (ui/children widget)
       (keep ui/user-object)
       set))
