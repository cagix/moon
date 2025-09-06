(ns cdq.editor.widget.one-to-many
  (:require [cdq.editor.overview-table]
            [cdq.db :as db]
            [cdq.property :as property]
            [cdq.utils :refer [pprint-to-str]]
            [cdq.image :as image]
            [cdq.ui.image :as ui.image]
            [clojure.vis-ui.tooltip :as tooltip]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.window :as scene2d.window]
            [cdq.ui.table :as table]
            [cdq.ui.text-button :as text-button]
            [cdq.ui.window :as window]))

(defn- add-one-to-many-rows
  [{:keys [ctx/db
           ctx/textures]}
   table
   property-type
   property-ids]
  (let [redo-rows (fn [ctx property-ids]
                    (group/clear-children! table)
                    (add-one-to-many-rows ctx table property-type property-ids)
                    (scene2d.window/pack-ancestors! table))]
    (table/add-rows!
     table
     [[(text-button/create "+"
                           (fn [_actor {:keys [ctx/stage] :as ctx}]
                             (let [window (window/create {:title "Choose"
                                                          :modal? true
                                                          :close-button? true
                                                          :center? true
                                                          :close-on-escape? true})
                                   clicked-id-fn (fn [id ctx]
                                                   (.remove window)
                                                   (redo-rows ctx (conj property-ids id)))]
                               (table/add! window (cdq.editor.overview-table/create ctx property-type clicked-id-fn))
                               (.pack window)
                               (stage/add! stage window))))]
      (for [property-id property-ids]
        (let [property (db/get-raw db property-id)
              texture-region (image/texture-region (property/image property) textures)
              image-widget (ui.image/create texture-region {:id property-id})]
          (tooltip/add! image-widget (pprint-to-str property))))
      (for [id property-ids]
        (text-button/create "-"
                            (fn [_actor ctx]
                              (redo-rows ctx (disj property-ids id)))))])))

(defn create [[_ property-type]  _attribute property-ids ctx]
  (let [table (table/create {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows ctx table property-type property-ids)
    table))

(defn value [_  _attribute widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       set))
