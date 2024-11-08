(ns moon.tx.audiovisual
  (:require [gdl.assets :refer [play-sound]]
            [moon.body :as body]
            [moon.db :as db]))

(defn handle [position id]
  (let [{:keys [tx/sound entity/animation]} (db/get id)]
    (play-sound sound)
    [[:e/create
      position
      body/effect-body-props
      {:entity/animation animation
       :entity/delete-after-animation-stopped true}]]))
