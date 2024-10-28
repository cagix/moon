(ns moon.tx.audiovisual
  (:require [moon.component :as component]
            [moon.db :as db]
            [moon.entity :as entity]))

(defc :tx/audiovisual
  (component/handle [[_ position id]]
    (let [{:keys [tx/sound entity/animation]} (db/get id)]
      [[:tx/sound sound]
       [:e/create
        position
        entity/effect-body-props
        {:entity/animation animation
         :entity/delete-after-animation-stopped? true}]])))
