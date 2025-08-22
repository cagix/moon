(ns cdq.ui.editor.widget.one-to-many
  (:require [cdq.ui.editor.overview-table]
            [cdq.ui.editor.widget :as widget]
            [cdq.db :as db]
            [cdq.property :as property]
            [cdq.utils :refer [pprint-to-str]]
            [gdl.graphics :as graphics]
            [gdx.ui.actor :as actor]
            [gdx.ui.group :as group]
            [gdl.ui.stage :as stage]
            [gdl.ui.table :as table]
            [gdx.ui :as ui]))

(defn- add-one-to-many-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-ids]
  (let [redo-rows (fn [ctx property-ids]
                    (group/clear-children! table)
                    (add-one-to-many-rows ctx table property-type property-ids)
                    (actor/pack-ancestor-window! table))]
    (table/add-rows!
     table
     [[(ui/text-button "+"
                       (fn [_actor {:keys [ctx/stage] :as ctx}]
                         (let [window (ui/window {:title "Choose"
                                                  :modal? true
                                                  :close-button? true
                                                  :center? true
                                                  :close-on-escape? true})
                               clicked-id-fn (fn [id ctx]
                                               (.remove window)
                                               (redo-rows ctx (conj property-ids id)))]
                           (table/add! window (cdq.ui.editor.overview-table/create ctx property-type clicked-id-fn))
                           (.pack window)
                           (stage/add! stage window))))]
      (for [property-id property-ids]
        (let [property (db/get-raw db property-id)
              texture-region (graphics/image->texture-region graphics (property/image property))
              image-widget (ui/image-widget texture-region
                                            {:id property-id})]
          (actor/add-tooltip! image-widget (pprint-to-str property))))
      (for [id property-ids]
        (ui/text-button "-"
                        (fn [_actor ctx]
                          (redo-rows ctx (disj property-ids id)))))])))

(defmethod widget/create :s/one-to-many [[_ property-type]  _attribute property-ids ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows ctx table property-type property-ids)
    table))

(defmethod widget/value :s/one-to-many [_  _attribute widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       set))
