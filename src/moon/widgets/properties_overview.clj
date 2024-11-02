(ns moon.widgets.properties-overview
  (:require [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.component :as component]
            [moon.db :as db]
            [moon.property :as property]))

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked #(clicked-id-fn id)
        button (if-let [image (property/->image props)]
                 (ui/image-button image on-clicked {:scale scale})
                 (ui/text-button (name id) on-clicked))
        top-widget (ui/label (or (and extra-info-text (extra-info-text props)) ""))
        stack (ui/stack [button top-widget])]
    (ui/add-tooltip! button #(component/->info props))
    (a/set-touchable! top-widget :disabled)
    stack))

(def ^:private overview
  {:properties/audiovisuals {:columns 10
                             :image/scale 2}
   :properties/creatures {:columns 15
                          :image/scale 1.5
                          :sort-by-fn #(vector (:creature/level %)
                                               (name (:creature/species %))
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
   :properties/worlds {:columns 10}})

(defc :widgets/properties-overview
  (component/create [[_ property-type clicked-id-fn]]
    (let [{:keys [sort-by-fn
                  extra-info-text
                  columns
                  image/scale]} (overview property-type)
          properties (db/all property-type)
          properties (if sort-by-fn
                       (sort-by sort-by-fn properties)
                       properties)]
      (ui/table
       {:cell-defaults {:pad 5}
        :rows (for [properties (partition-all columns properties)]
                (for [property properties]
                  (try (property-widget property clicked-id-fn extra-info-text scale)
                       (catch Throwable t
                         (throw (ex-info "" {:property property} t))))))}))))
