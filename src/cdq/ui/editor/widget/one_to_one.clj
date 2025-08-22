(ns cdq.ui.editor.widget.one-to-one
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

(defn- add-one-to-one-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-id]
  (let [redo-rows (fn [ctx id]
                    (group/clear-children! table)
                    (add-one-to-one-rows ctx table property-type id)
                    (actor/pack-ancestor-window! table))]
    (table/add-rows!
     table
     [[(when-not property-id
         (ui/text-button "+"
                         (fn [_actor {:keys [ctx/stage] :as ctx}]
                           (let [window (ui/window {:title "Choose"
                                                    :modal? true
                                                    :close-button? true
                                                    :center? true
                                                    :close-on-escape? true})
                                 clicked-id-fn (fn [id ctx]
                                                 (.remove window)
                                                 (redo-rows ctx id))]
                             (table/add! window (cdq.ui.editor.overview-table/create ctx property-type clicked-id-fn))
                             (.pack window)
                             (stage/add! stage window)))))]
      [(when property-id
         (let [property (db/get-raw db property-id)
               texture-region (graphics/image->texture-region graphics (property/image property))
               image-widget (ui/image-widget texture-region
                                             {:id property-id})]
           (actor/add-tooltip! image-widget (pprint-to-str property))
           image-widget))]
      [(when property-id
         (ui/text-button "-"
                         (fn [_actor ctx]
                           (redo-rows ctx nil))))]])))

(defmethod widget/create :s/one-to-one [[_ property-type]  _attribute property-id ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows ctx table property-type property-id)
    table))

(defmethod widget/value :s/one-to-one [_  _attribute widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       first))
