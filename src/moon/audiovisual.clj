(ns ^:no-doc moon.audiovisual
  (:require [moon.component :refer [defc]]
            [moon.db :as db]
            [moon.property :as property]
            [moon.tx :as tx]
            [moon.entity :as entity]))

(property/def :properties/audiovisuals
  {:schema [:tx/sound
            :entity/animation]
   :overview {:title "Audiovisuals"
              :columns 10
              :image/scale 2}})

(defc :tx/audiovisual
  (tx/handle [[_ position id]]
    (let [{:keys [tx/sound entity/animation]} (db/get id)]
      [[:tx/sound sound]
       [:e/create
        position
        entity/effect-body-props
        {:entity/animation animation
         :entity/delete-after-animation-stopped? true}]])))

(defc :entity/destroy-audiovisual
  {:let audiovisuals-id}
  (entity/destroy [_ eid]
    [[:tx/audiovisual (:position @eid) audiovisuals-id]]))

