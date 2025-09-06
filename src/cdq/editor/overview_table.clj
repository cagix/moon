(ns cdq.editor.overview-table
  (:require [cdq.db :as db]
            [cdq.property :as property]
            [cdq.image :as image]
            [cdq.ui.image-button :as image-button]
            [cdq.ui.label :as label]
            [cdq.ui.text-button :as text-button]
            [cdq.ui.tooltip :as tooltip]
            [cdq.utils :refer [pprint-to-str]]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

; FIXME not refreshed after changes in properties

(def ^:private overview {:properties/audiovisuals {:columns 10
                                                   :image/scale 2}
                         :properties/creatures {:columns 15
                                                :image/scale 1.5
                                                :sort-by-fn #(vector (:creature/level %)
                                                                     (name (:entity/species %))
                                                                     (name (:property/id %)))
                                                :extra-info-text #(str (:creature/level %))}
                         :properties/items {:columns 20
                                            :image/scale 1.1
                                            :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                                                   (name slot)
                                                                   "")
                                                                 (name (:property/id %)))}
                         :properties/projectiles {:columns 16
                                                  :image/scale 2}
                         :properties/skills {:columns 16
                                             :image/scale 2}
                         :properties/worlds {:columns 10}
                         :properties/player-dead {:columns 1}
                         :properties/player-idle {:columns 1}
                         :properties/player-item-on-cursor {:columns 1}})

; TODO complecting ui stuff with db stuff !!!
; dumb ui containers
; editor.ui/
(defn create
  [{:keys [ctx/db
           ctx/textures]}
   property-type
   clicked-id-fn]
  (assert (contains? overview property-type)
          (pr-str property-type))
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (overview property-type)
        properties (db/all-raw db property-type)
        properties (if sort-by-fn
                     (sort-by sort-by-fn properties)
                     properties)]
    {:actor/type :actor.type/table
     :cell-defaults {:pad 5}
     :rows (for [properties (partition-all columns properties)]
             (for [{:keys [property/id] :as property} properties]
               (try (let [on-clicked (fn [_actor ctx]
                                       (clicked-id-fn id ctx))
                          button (if-let [image (property/image property)]
                                   (image-button/create
                                    {:drawable/texture-region (image/texture-region image textures)
                                     :on-clicked on-clicked
                                     :drawable/scale scale})
                                   (text-button/create (name id)
                                                       on-clicked))
                          top-widget (label/create {:label/text (or (and extra-info-text
                                                                         (extra-info-text property))
                                                                    "")})
                          stack {:actor/type :actor.type/stack
                                 :actors [button
                                          top-widget]}]
                      (tooltip/add! button (pprint-to-str property))
                      (actor/set-touchable! top-widget :disabled)
                      {:actor stack})
                    (catch Throwable t
                      (throw (ex-info "" {:property property} t))))))}))
