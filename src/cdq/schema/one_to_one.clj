(ns cdq.schema.one-to-one
  (:require [cdq.ctx.db :as db]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.ctx.graphics :as graphics]
            [cdq.ui.editor.property :as property]
            [cdq.property]
            [cdq.string :as string]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [clojure.gdx.scene2d.ui.window :as window]
            [clojure.vis-ui.tooltip :as tooltip]))

(defn create-value [_ property-id db]
  (db/build db property-id))

(defn malli-form [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (cdq.property/type->id-namespace property-type)}])

(defn- add-one-to-one-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-id]
  (let [redo-rows (fn [ctx id]
                    (group/clear-children! table)
                    (add-one-to-one-rows ctx table property-type id)
                    (window/pack-ancestors! table))]
    (table/add-rows!
     table
     [[(when-not property-id
         {:actor {:actor/type :actor.type/text-button
                  :text "+"
                  :on-clicked (fn [_actor {:keys [ctx/stage] :as ctx}]
                                (let [window (scene2d/build
                                              {:actor/type :actor.type/window
                                               :title "Choose"
                                               :modal? true
                                               :close-button? true
                                               :center? true
                                               :close-on-escape? true})
                                      clicked-id-fn (fn [id ctx]
                                                      (.remove window)
                                                      (redo-rows ctx id))]
                                  (table/add! window (overview-table/create ctx property-type clicked-id-fn))
                                  (.pack window)
                                  (stage/add! stage window)))}})]
      [(when property-id
         (let [property (db/get-raw db property-id)
               texture-region (graphics/texture-region graphics (property/image property))
               image-widget (scene2d/build
                             {:actor/type :actor.type/image
                              :image/object texture-region
                              :actor/user-object property-id})]
           {:actor (tooltip/add! image-widget (string/pprint-to-str property))}
           image-widget))]
      [(when property-id
         {:actor {:actor/type :actor.type/text-button
                  :text "-"
                  :on-clicked (fn [_actor ctx]
                                (redo-rows ctx nil))}})]])))

(defn create [[_ property-type] property-id ctx]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}})]
    (add-one-to-one-rows ctx table property-type property-id)
    table))

(defn value [_  widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       first))
