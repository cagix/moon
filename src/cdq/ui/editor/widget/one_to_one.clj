(ns cdq.ui.editor.widget.one-to-one
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.info :as info]
            [cdq.property :as property]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [pprint-to-str]]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.ui.stage :as stage])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn- add-one-to-one-rows [table property-type property-id]
  (let [redo-rows (fn [id]
                    (Group/.clearChildren table)
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
                                                 (.remove window)
                                                 (redo-rows id))]
                             (Table/.add window ^Actor (overview-table/create property-type clicked-id-fn))
                             (.pack window)
                             (stage/add-actor! ctx/stage window)))))]
      [(when property-id
         (let [property (db/build ctx/db property-id)
               image-widget (ui/image->widget (property/image property)
                                              {:id property-id})]
           (actor/add-tooltip! image-widget #(pprint-to-str property))
           image-widget))]
      [(when property-id
         (ui/text-button "-" #(redo-rows nil)))]])))

(defmethod widget/create :s/one-to-one [[_ property-type] property-id]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows table property-type property-id)
    table))

(defmethod widget/value :s/one-to-one [_ widget]
  (->> (Group/.getChildren widget)
       (keep Actor/.getUserObject)
       first))
