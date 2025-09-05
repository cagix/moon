(ns cdq.editor.widget.one-to-one
  (:require [cdq.editor.overview-table]
            [cdq.db :as db]
            [cdq.property :as property]
            [cdq.utils :refer [pprint-to-str]]
            [cdq.image :as image]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.image :as ui.image]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [cdq.ui.table :as table]
            [cdq.ui.text-button :as text-button]
            [cdq.ui :as ui]))

(defn- add-one-to-one-rows
  [{:keys [ctx/db
           ctx/textures]}
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
         (text-button/create "+"
                             (fn [_actor {:keys [ctx/stage] :as ctx}]
                               (let [window (ui/window {:title "Choose"
                                                        :modal? true
                                                        :close-button? true
                                                        :center? true
                                                        :close-on-escape? true})
                                     clicked-id-fn (fn [id ctx]
                                                     (.remove window)
                                                     (redo-rows ctx id))]
                                 (table/add! window (cdq.editor.overview-table/create ctx property-type clicked-id-fn))
                                 (.pack window)
                                 (stage/add! stage window)))))]
      [(when property-id
         (let [property (db/get-raw db property-id)
               texture-region (image/texture-region (property/image property) textures)
               image-widget (ui.image/create texture-region
                                             {:id property-id})]
           (actor/add-tooltip! image-widget (pprint-to-str property))
           image-widget))]
      [(when property-id
         (text-button/create "-"
                             (fn [_actor ctx]
                               (redo-rows ctx nil))))]])))

(defn create [[_ property-type]  _attribute property-id ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows ctx table property-type property-id)
    table))

(defn value [_  _attribute widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       first))
