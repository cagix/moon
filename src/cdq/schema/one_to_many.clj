(ns cdq.schema.one-to-many
  (:require [cdq.db :as db]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.graphics :as graphics]
            [cdq.ui.editor.property :as property]
            [cdq.property]
            [cdq.string :as string]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [clojure.gdx.scene2d.ui.window :as window]
            [com.kotcrab.vis.ui.widget.tooltip :as tooltip]))

(defn malli-form [[_ property-type] _schemas]
  [:set [:qualified-keyword {:namespace (cdq.property/type->id-namespace property-type)}]])

(defn create-value [_ property-ids db]
  (set (map (partial db/build db) property-ids)))

(defn- add-one-to-many-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-ids]
  (let [redo-rows (fn [ctx property-ids]
                    (group/clear-children! table)
                    (add-one-to-many-rows ctx table property-type property-ids)
                    (window/pack-ancestors! table))]
    (table/add-rows!
     table
     [[{:actor {:actor/type :actor.type/text-button
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
                                                    (redo-rows ctx (conj property-ids id)))]
                                (table/add-rows! window (overview-table/create ctx property-type clicked-id-fn))
                                (.pack window)
                                (stage/add! stage window)))}}]
      (for [property-id property-ids]
        (let [property (db/get-raw db property-id)
              texture-region (graphics/texture-region graphics (property/image property))
              image-widget (scene2d/build
                            {:actor/type :actor.type/image
                             :image/object texture-region
                             :actor/user-object property-id})]
          {:actor (tooltip/add! image-widget (string/pprint-to-str property))}))
      (for [id property-ids]
        {:actor {:actor/type :actor.type/text-button
                 :text "-"
                 :on-clicked (fn [_actor ctx]
                               (redo-rows ctx (disj property-ids id)))}})])))

(defn create [[_ property-type] property-ids ctx]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}})]
    (add-one-to-many-rows ctx table property-type property-ids)
    table))

(defn value [_  widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       set))
