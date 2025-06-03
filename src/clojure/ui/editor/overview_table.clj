(ns clojure.ui.editor.overview-table
  (:require [clojure.ui :as ui]
            [clojure.db :as db]
            [clojure.property :as property]
            [clojure.utils :refer [pprint-to-str]]))

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

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked (fn [_actor ctx]
                     (clicked-id-fn id ctx))
        button (if-let [image (property/image props)]
                 (ui/image-button (:texture-region image)
                                  on-clicked
                                  {:scale scale})
                 (ui/text-button (name id)
                                 on-clicked))
        top-widget (ui/label (or (and extra-info-text
                                      (extra-info-text props))
                                 ""))
        stack (ui/stack [button
                         top-widget])]
    (ui/add-tooltip! button (pprint-to-str props))
    (ui/set-touchable! top-widget :disabled)
    stack))

(defn create [{:keys [ctx/db]
               :as ctx}
              property-type clicked-id-fn]
  (assert (contains? overview property-type)
          (pr-str property-type))
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (overview property-type)
        properties (db/build-all db property-type ctx)
        properties (if sort-by-fn
                     (sort-by sort-by-fn properties)
                     properties)]
    (ui/table
     {:cell-defaults {:pad 5}
      :rows (for [properties (partition-all columns properties)]
              (for [property properties]
                (try (property-widget property clicked-id-fn extra-info-text scale)
                     (catch Throwable t
                       (throw (ex-info "" {:property property} t))))))})))
